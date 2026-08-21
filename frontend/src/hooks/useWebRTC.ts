import { useCallback, useRef, useState } from 'react'
import type { IceServer } from '../types'

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

      const pc = new RTCPeerConnection({ iceServers: iceServers as RTCIceServer[] })
      pcRef.current = pc

      localStream.getTracks().forEach((track) => pc.addTrack(track, localStream))

      pc.onicecandidate = (event) => {
        if (event.candidate) {
          onIceCandidate(event.candidate.toJSON())
        }
      }

      pc.ontrack = (event) => {
        setRemoteStream(event.streams[0])
      }

      pc.onconnectionstatechange = () => {
        const state = pc.connectionState as PeerConnectionState
        setConnectionState(state)
        onConnectionStateChange?.(state)
      }

      pc.oniceconnectionstatechange = () => {
        // Attempt an ICE restart once on failure before giving up - covers
        // transient network blips (e.g. wifi -> cellular handoff).
        if (pc.iceConnectionState === 'failed') {
          pc.restartIce()
        }
      }

      return pc
    },
    [onIceCandidate, onConnectionStateChange]
  )

  const createOffer = useCallback(async (): Promise<RTCSessionDescriptionInit> => {
    const pc = pcRef.current
    if (!pc) throw new Error('No active peer connection')
    const offer = await pc.createOffer()
    await pc.setLocalDescription(offer)
    return offer
  }, [])

  const createAnswer = useCallback(async (offer: RTCSessionDescriptionInit): Promise<RTCSessionDescriptionInit> => {
    const pc = pcRef.current
    if (!pc) throw new Error('No active peer connection')
    await pc.setRemoteDescription(offer)
    await flushPendingCandidates()
    const answer = await pc.createAnswer()
    await pc.setLocalDescription(answer)
    return answer
  }, [])

  const acceptAnswer = useCallback(async (answer: RTCSessionDescriptionInit) => {
    const pc = pcRef.current
    if (!pc) throw new Error('No active peer connection')
    await pc.setRemoteDescription(answer)
    await flushPendingCandidates()
  }, [])

  const addRemoteIceCandidate = useCallback(async (candidate: RTCIceCandidateInit) => {
    const pc = pcRef.current
    if (!pc || !pc.remoteDescription) {
      // Buffer candidates that arrive before the remote description is set
      // (common when ICE gathering starts fast on the offering side).
      pendingCandidates.current.push(candidate)
      return
    }
    try {
      await pc.addIceCandidate(candidate)
    } catch {
      // Ignore malformed/late candidates rather than crashing the call.
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
      } catch {
        /* ignore */
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
