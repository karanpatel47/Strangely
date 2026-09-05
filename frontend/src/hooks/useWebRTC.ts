import { useCallback, useRef, useState } from 'react'
import type { IceServer } from '../types'
import { dlog } from '../utils/debug'

export type PeerConnectionState = 'new' | 'connecting' | 'connected' | 'disconnected' | 'failed' | 'closed'

interface Options {
  onIceCandidate: (candidate: RTCIceCandidateInit) => void
  onConnectionStateChange?: (state: PeerConnectionState) => void
}

/**
 * Manages exactly one RTCPeerConnection at a time. The peer connection is
 * torn down and recreated for every new match (Next button) rather than
 * reused, which is simpler and avoids subtle stale-negotiation bugs.
 */
export function useWebRTC({ onIceCandidate, onConnectionStateChange }: Options) {
  const pcRef = useRef<RTCPeerConnection | null>(null)
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null)
  const [connectionState, setConnectionState] = useState<PeerConnectionState>('new')
  const pendingCandidates = useRef<RTCIceCandidateInit[]>([])

  const createPeerConnection = useCallback(
    (iceServers: IceServer[], localStream: MediaStream) => {
      closePeerConnection()

      dlog('[WebRTC] ICE servers:', iceServers.map(({ urls, username, credential }) => ({
        urls,
        hasUsername: Boolean(username),
        hasCredential: Boolean(credential)
      })))
      const pc = new RTCPeerConnection({ iceServers: iceServers as RTCIceServer[] })
      pcRef.current = pc

      localStream.getTracks().forEach((track) => pc.addTrack(track, localStream))
      dlog('[WebRTC] local tracks:', pc.getSenders())

      pc.onicecandidate = (event) => {
        if (event.candidate) {
          dlog('[WebRTC] onicecandidate')
          onIceCandidate(event.candidate.toJSON())
        }
      }

      pc.ontrack = (event) => {
        dlog('[WebRTC] ontrack:', event.streams)
        const stream = event.streams[0] ?? new MediaStream([event.track])
        setRemoteStream(stream)
        dlog('[WebRTC] remote tracks:', pc.getReceivers())
      }

      pc.onconnectionstatechange = () => {
        const state = pc.connectionState as PeerConnectionState
        dlog('[WebRTC] connectionState:', pc.connectionState)
        setConnectionState(state)
        onConnectionStateChange?.(state)
      }

      pc.onsignalingstatechange = () => dlog('[WebRTC] signalingState:', pc.signalingState)
      pc.onicegatheringstatechange = () => dlog('[WebRTC] iceGatheringState:', pc.iceGatheringState)
      pc.oniceconnectionstatechange = () => {
        dlog('[WebRTC] iceConnectionState:', pc.iceConnectionState)
      }

      dlog('[WebRTC] signalingState:', pc.signalingState)
      dlog('[WebRTC] iceGatheringState:', pc.iceGatheringState)
      dlog('[WebRTC] iceConnectionState:', pc.iceConnectionState)

      return pc
    },
    [onIceCandidate, onConnectionStateChange]
  )

  const createOffer = useCallback(async (): Promise<RTCSessionDescriptionInit> => {
    const pc = pcRef.current
    if (!pc) throw new Error('No active peer connection')
    try {
      const offer = await pc.createOffer()
      await pc.setLocalDescription(offer)
      dlog('[WebRTC] offer created and local description set')
      return offer
    } catch (error) {
      console.error('[WebRTC] createOffer/setLocalDescription failed', error)
      throw error
    }
  }, [])

  const createAnswer = useCallback(async (offer: RTCSessionDescriptionInit): Promise<RTCSessionDescriptionInit> => {
    const pc = pcRef.current
    if (!pc) throw new Error('No active peer connection')
    try {
      await pc.setRemoteDescription(offer)
      await flushPendingCandidates()
      const answer = await pc.createAnswer()
      await pc.setLocalDescription(answer)
      dlog('[WebRTC] remote offer accepted; answer created and local description set')
      return answer
    } catch (error) {
      console.error('[WebRTC] createAnswer/setRemoteDescription failed', error)
      throw error
    }
  }, [])

  const acceptAnswer = useCallback(async (answer: RTCSessionDescriptionInit) => {
    const pc = pcRef.current
    if (!pc) throw new Error('No active peer connection')
    try {
      await pc.setRemoteDescription(answer)
      await flushPendingCandidates()
      dlog('[WebRTC] remote answer accepted')
    } catch (error) {
      console.error('[WebRTC] setRemoteDescription(answer) failed', error)
      throw error
    }
  }, [])

  const addRemoteIceCandidate = useCallback(async (candidate: RTCIceCandidateInit) => {
    const pc = pcRef.current
    if (!pc || !pc.remoteDescription) {
      // Buffer candidates that arrive before the remote description is set
      // (common when ICE gathering starts fast on the offering side).
      pendingCandidates.current.push(candidate)
      dlog('[WebRTC] queued ICE candidate until remote description is set')
      return
    }
    try {
      await pc.addIceCandidate(candidate)
      dlog('[WebRTC] remote ICE candidate added')
    } catch (error) {
      console.error('[WebRTC] addIceCandidate failed', error)
      throw error
    }
  }, [])

  async function flushPendingCandidates() {
    const pc = pcRef.current
    if (!pc) return
    const queued = pendingCandidates.current
    pendingCandidates.current = []
    for (const c of queued) {
      try {
        await pc.addIceCandidate(c)
        dlog('[WebRTC] queued ICE candidate added')
      } catch (error) {
        console.error('[WebRTC] queued addIceCandidate failed', error)
        throw error
      }
    }
  }

  const setVideoEnabled = useCallback((enabled: boolean, localStream: MediaStream | null) => {
    localStream?.getVideoTracks().forEach((t) => (t.enabled = enabled))
  }, [])

  const setAudioEnabled = useCallback((enabled: boolean, localStream: MediaStream | null) => {
    localStream?.getAudioTracks().forEach((t) => (t.enabled = enabled))
  }, [])

  const closePeerConnection = useCallback(() => {
    pendingCandidates.current = []
    if (pcRef.current) {
      pcRef.current.onicecandidate = null
      pcRef.current.ontrack = null
      pcRef.current.onconnectionstatechange = null
      pcRef.current.oniceconnectionstatechange = null
      pcRef.current.close()
      pcRef.current = null
    }
    setRemoteStream(null)
    setConnectionState('closed')
  }, [])

  return {
    createPeerConnection,
    createOffer,
    createAnswer,
    acceptAnswer,
    addRemoteIceCandidate,
    setVideoEnabled,
    setAudioEnabled,
    closePeerConnection,
    remoteStream,
    connectionState
  }
}
