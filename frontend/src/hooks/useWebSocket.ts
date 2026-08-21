import { useEffect, useRef, useState } from 'react'
import { signalingClient } from '../services/websocket'
import type { ChatMessageDto, ErrorMessage, RoomEventMessage, SignalMessage } from '../types'

export type WsStatus = 'connecting' | 'connected' | 'disconnected' | 'failed'

interface Callbacks {
  onRoomEvent: (msg: RoomEventMessage) => void
  onSignal: (msg: SignalMessage) => void
  onChat: (msg: ChatMessageDto) => void
  onError: (msg: ErrorMessage) => void
}

/**
 * Owns the lifecycle of the single STOMP connection for this tab. Callbacks
 * are stored in a ref so callers can pass fresh closures every render
 * without re-subscribing the socket.
 */
export function useWebSocket(callbacks: Callbacks) {
  const [status, setStatus] = useState<WsStatus>('connecting')
  const callbacksRef = useRef(callbacks)
  callbacksRef.current = callbacks

  useEffect(() => {
    let cancelled = false
    setStatus('connecting')

    signalingClient
      .connect({
        onConnected: () => !cancelled && setStatus('connected'),
        onDisconnected: () => !cancelled && setStatus('disconnected'),
        onRoomEvent: (m) => callbacksRef.current.onRoomEvent(m),
        onSignal: (m) => callbacksRef.current.onSignal(m),
        onChat: (m) => callbacksRef.current.onChat(m),
        onError: (m) => callbacksRef.current.onError(m)
      })
      .catch(() => !cancelled && setStatus('failed'))

    return () => {
      cancelled = true
      signalingClient.disconnect()
    }
  }, [])

  return { status }
}
