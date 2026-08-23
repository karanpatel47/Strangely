import { useCallback, useEffect, useRef, useState } from 'react'
import { signalingClient } from '../services/websocket'
import { fetchIceServers } from '../services/api'
import { classifyMediaError, getLocalMedia, stopStream } from '../utils/webrtc'
import { useWebRTC } from './useWebRTC'
import { useWebSocket } from './useWebSocket'
import type { CallState, ChatEntry, ChatMessageDto, IceServer, RoomEventMessage, SignalMessage } from '../types'

export function useMatchmaking() {
  const [callState, setCallState] = useState<CallState>('idle')
  const [localStream, setLocalStream] = useState<MediaStream | null>(null)
  const [micOn, setMicOn] = useState(true)
  const [camOn, setCamOn] = useState(true)
  const [chatLog, setChatLog] = useState<ChatEntry[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const roomIdRef = useRef<string | null>(null)
  const peerIdRef = useRef<string | null>(null)
  const iceServersRef = useRef<IceServer[]>([{ urls: ['stun:stun.l.google.com:19302'] }])
  const localStreamRef = useRef<MediaStream | null>(null)

  const webrtc = useWebRTC({
    onIceCandidate: (candidate) => {
      if (!roomIdRef.current) return
      console.log('[WebRTC] sending ICE candidate')
      signalingClient.sendIceCandidate({
        roomId: roomIdRef.current,
        type: 'ICE_CANDIDATE',
        payload: JSON.stringify(candidate)
      })
    },
    onConnectionStateChange: (state) => {
      if (state === 'connected') setCallState('connected')
      if (state === 'failed') setErrorMessage('Connection to stranger failed')
    }
  })

  const handleRoomEvent = useCallback(async (msg: RoomEventMessage) => {
    switch (msg.type) {
      case 'WAITING':
        setCallState('searching')
        break

      case 'MATCH_FOUND': {
        roomIdRef.current = msg.roomId
        peerIdRef.current = msg.peerId
        setChatLog([])
        setCallState('connecting_call')

        if (!localStreamRef.current) return
        webrtc.createPeerConnection(iceServersRef.current, localStreamRef.current)

        if (msg.initiator) {
          try {
            const offer = await webrtc.createOffer()
            console.log('[WebRTC] sending offer')
            signalingClient.sendOffer({ roomId: msg.roomId!, type: 'OFFER', payload: JSON.stringify(offer) })
          } catch (error) {
            console.error('[WebRTC] offer setup failed', error)
            setErrorMessage('Could not start the video connection')
          }
        }
        break
      }

      case 'PEER_LEFT':
      case 'PEER_NEXT':
        webrtc.closePeerConnection()
        setCallState('peer_left')
        setChatLog((log) => [
          ...log,
          { id: crypto.randomUUID(), sender: 'stranger', content: '— stranger has disconnected —', timestamp: Date.now() }
        ])
        break

      case 'CALL_ENDED':
        webrtc.closePeerConnection()
        setCallState('call_ended')
        break
    }
  }, [webrtc])

  const handleSignal = useCallback(
    async (msg: SignalMessage) => {
      try {
        if (msg.roomId !== roomIdRef.current) {
          console.warn('[WebRTC] ignoring signal for inactive room')
          return
        }
        if (msg.type === 'OFFER') {
          const offer = JSON.parse(msg.payload) as RTCSessionDescriptionInit
          const answer = await webrtc.createAnswer(offer)
          console.log('[WebRTC] sending answer')
          signalingClient.sendAnswer({ roomId: msg.roomId, type: 'ANSWER', payload: JSON.stringify(answer) })
        } else if (msg.type === 'ANSWER') {
          const answer = JSON.parse(msg.payload) as RTCSessionDescriptionInit
          await webrtc.acceptAnswer(answer)
        } else if (msg.type === 'ICE_CANDIDATE') {
          console.log('[WebRTC] received ICE candidate')
          const candidate = JSON.parse(msg.payload) as RTCIceCandidateInit
          await webrtc.addRemoteIceCandidate(candidate)
        }
      } catch (error) {
        console.error('[WebRTC] signal handling failed', error)
        setErrorMessage('Video signaling failed')
      }
    },
    [webrtc]
  )

  const handleChat = useCallback((msg: ChatMessageDto) => {
    const isMine = msg.senderId === signalingClient.getUserId()
    setChatLog((log) => [
      ...log,
      {
        id: crypto.randomUUID(),
        sender: isMine ? 'me' : 'stranger',
        content: msg.content,
        timestamp: msg.timestamp || Date.now()
      }
    ])
  }, [])

  const handleWsError = useCallback((msg: { code: string; message: string }) => {
    setErrorMessage(msg.message)
  }, [])

  const { status: wsStatus } = useWebSocket({
    onRoomEvent: handleRoomEvent,
    onSignal: handleSignal,
    onChat: handleChat,
    onError: handleWsError
  })

  // Acquire camera/mic + ICE server config once on mount.
  useEffect(() => {
    let cancelled = false
    setCallState('requesting_media')

    ;(async () => {
      try {
        const stream = await getLocalMedia()
        if (cancelled) {
          stopStream(stream)
          return
        }
        localStreamRef.current = stream
        setLocalStream(stream)
      } catch (err) {
        const kind = classifyMediaError(err)
        setErrorMessage(
          kind === 'permission_denied'
            ? 'Camera/microphone access was denied. Allow access in your browser settings and reload.'
            : 'Could not access your camera or microphone. Check that no other app is using them.'
        )
        setCallState('media_denied')
        return
      }

      try {
        const config = await fetchIceServers()
        console.log('[WebRTC] fetched ICE config:', config.iceServers.map(({ urls, username, credential }) => ({
          urls,
          hasUsername: Boolean(username),
          hasCredential: Boolean(credential)
        })))
        iceServersRef.current = config.iceServers
        setCallState('connecting_ws')
      } catch (error) {
        console.error('[WebRTC] ICE config fetch failed', error)
        setErrorMessage('Could not load video connection settings')
        setCallState('error')
      }
    })()

    return () => {
      cancelled = true
      stopStream(localStreamRef.current)
      webrtc.closePeerConnection()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // Once media + socket are both ready, enter the queue automatically.
  useEffect(() => {
    if (callState === 'connecting_ws' && wsStatus === 'connected') {
      signalingClient.findMatch()
      setCallState('searching')
    }
    if (wsStatus === 'failed') {
      setErrorMessage('Could not reach the server. Check your connection and try again.')
    }
  }, [callState, wsStatus])

  const findNext = useCallback(() => {
    webrtc.closePeerConnection()
    setChatLog([])
    roomIdRef.current = null
    peerIdRef.current = null
    setCallState('searching')
    signalingClient.next()
  }, [webrtc])

  const startSearchAgain = useCallback(() => {
    setChatLog([])
    setCallState('searching')
    signalingClient.findMatch()
  }, [])

  const endCall = useCallback(() => {
    webrtc.closePeerConnection()
    roomIdRef.current = null
    peerIdRef.current = null
    setCallState('call_ended')
    signalingClient.endCall()
  }, [webrtc])

  const toggleMic = useCallback(() => {
    setMicOn((prev) => {
      const next = !prev
      webrtc.setAudioEnabled(next, localStreamRef.current)
      return next
    })
  }, [webrtc])

  const toggleCam = useCallback(() => {
    setCamOn((prev) => {
      const next = !prev
      webrtc.setVideoEnabled(next, localStreamRef.current)
      return next
    })
  }, [webrtc])

  const sendChatMessage = useCallback((content: string) => {
    if (!content.trim() || !roomIdRef.current) return
    signalingClient.sendChat({ roomId: roomIdRef.current, content: content.trim() })
  }, [])

  return {
    callState,
    wsStatus,
    localStream,
    remoteStream: webrtc.remoteStream,
    micOn,
    camOn,
    chatLog,
    errorMessage,
    clearError: () => setErrorMessage(null),
    findNext,
    startSearchAgain,
    endCall,
    toggleMic,
    toggleCam,
    sendChatMessage
  }
}
