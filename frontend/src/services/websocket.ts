import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { ChatMessageDto, ErrorMessage, RoomEventMessage, SignalMessage } from '../types'

const WS_URL = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'

type Handlers = {
  onRoomEvent?: (msg: RoomEventMessage) => void
  onSignal?: (msg: SignalMessage) => void
  onChat?: (msg: ChatMessageDto) => void
  onError?: (msg: ErrorMessage) => void
  onConnected?: () => void
  onDisconnected?: () => void
}

/**
 * Thin wrapper around @stomp/stompjs. Owns exactly one connection per app
 * session, persists a userId in localStorage so a page refresh reconnects
 * as "the same" anonymous user (useful for presence/session continuity),
 * and exposes typed send/subscribe methods matching the backend protocol
 * documented in WebSocketConfig.java.
 */
export class SignalingClient {
  private client: Client | null = null
  private handlers: Handlers = {}
  private subs: StompSubscription[] = []

  getUserId(): string {
    let id = localStorage.getItem('stranger-chat-user-id')
    if (!id) {
      id = crypto.randomUUID()
      localStorage.setItem('stranger-chat-user-id', id)
    }
    return id
  }

  connect(handlers: Handlers): Promise<void> {
    this.handlers = handlers
    const userId = this.getUserId()

    return new Promise((resolve, reject) => {
      const client = new Client({
        webSocketFactory: () => new SockJS(`${WS_URL}?userId=${encodeURIComponent(userId)}`),
        reconnectDelay: 3000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
          this.subscribeAll()
          this.handlers.onConnected?.()
          resolve()
        },
        onStompError: (frame) => {
          this.handlers.onError?.({ code: 'STOMP_ERROR', message: frame.headers['message'] || 'Signaling error' })
        },
        onWebSocketClose: () => {
          this.handlers.onDisconnected?.()
        },
        onDisconnect: () => {
          this.handlers.onDisconnected?.()
        }
      })

      client.activate()
      this.client = client

      // Fail fast if we never connect (e.g. backend down)
      setTimeout(() => {
        if (!client.connected) {
          reject(new Error('Could not reach signaling server'))
        }
      }, 8000)
    })
  }

  private subscribeAll() {
    if (!this.client) return
    this.subs.push(
      this.client.subscribe('/user/queue/room', (m: IMessage) => {
        this.handlers.onRoomEvent?.(JSON.parse(m.body))
      }),
      this.client.subscribe('/user/queue/signal', (m: IMessage) => {
        this.handlers.onSignal?.(JSON.parse(m.body))
      }),
      this.client.subscribe('/user/queue/chat', (m: IMessage) => {
        this.handlers.onChat?.(JSON.parse(m.body))
      }),
      this.client.subscribe('/user/queue/errors', (m: IMessage) => {
        this.handlers.onError?.(JSON.parse(m.body))
      })
    )
  }

  findMatch() {
    this.publish('/app/match/find', {})
  }

  next() {
    this.publish('/app/match/next', {})
  }

  endCall() {
    this.publish('/app/call/end', {})
  }

  sendOffer(msg: SignalMessage) {
    this.publish('/app/call/offer', msg)
  }

  sendAnswer(msg: SignalMessage) {
    this.publish('/app/call/answer', msg)
  }

  sendIceCandidate(msg: SignalMessage) {
    this.publish('/app/call/ice', msg)
  }

  sendChat(msg: ChatMessageDto) {
    this.publish('/app/chat/send', msg)
  }

  private publish(destination: string, body: unknown) {
    if (!this.client?.connected) {
      this.handlers.onError?.({ code: 'NOT_CONNECTED', message: 'Not connected to signaling server' })
      return
    }
    this.client.publish({ destination, body: JSON.stringify(body) })
  }

  isConnected(): boolean {
    return !!this.client?.connected
  }

  disconnect() {
    this.subs.forEach((s) => s.unsubscribe())
    this.subs = []
    this.client?.deactivate()
    this.client = null
  }
}

export const signalingClient = new SignalingClient()
