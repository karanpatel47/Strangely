import { Client, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type {
  ChatMessageDto,
  ErrorMessage,
  RoomEventMessage,
  SignalMessage,
} from '../types'

const WS_URL =
  import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'

type Handlers = {
  onRoomEvent?: (msg: RoomEventMessage) => void
  onSignal?: (msg: SignalMessage) => void
  onChat?: (msg: ChatMessageDto) => void
  onError?: (msg: ErrorMessage) => void
  onConnected?: () => void
  onDisconnected?: () => void
}

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
      let settled = false

      const client = new Client({
        webSocketFactory: () => {
          const sockJsUrl =
            `${WS_URL}?userId=${encodeURIComponent(userId)}`

          return new SockJS(sockJsUrl)
        },

        reconnectDelay: 3000,

        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,

        onConnect: () => {
          settled = true

          this.subscribeAll()

          this.handlers.onConnected?.()

          resolve()
        },

        onStompError: (frame) => {
          this.handlers.onError?.({
            code: 'STOMP_ERROR',
            message:
              frame.headers['message'] ||
              'Signaling error',
          })

          if (!settled) {
            settled = true
            reject(
              new Error(
                frame.headers['message'] ||
                'Signaling server error'
              )
            )
          }
        },

        onWebSocketClose: () => {
          this.handlers.onDisconnected?.()
        },

        onDisconnect: () => {
          this.handlers.onDisconnected?.()
        },

        onWebSocketError: () => {
          this.handlers.onError?.({
            code: 'WEBSOCKET_ERROR',
            message: 'WebSocket connection error',
          })
        },
      })

      client.activate()

      this.client = client

      setTimeout(() => {
        if (!client.connected && !settled) {
          settled = true

          reject(
            new Error(
              'Could not reach signaling server'
            )
          )
        }
      }, 10000)
    })
  }

  private subscribeAll() {
    if (!this.client) {
      return
    }

    this.subs.push(
      this.client.subscribe(
        '/user/queue/room',
        (message: IMessage) => {
          this.handlers.onRoomEvent?.(
            JSON.parse(message.body)
          )
        }
      ),

      this.client.subscribe(
        '/user/queue/signal',
        (message: IMessage) => {
          this.handlers.onSignal?.(
            JSON.parse(message.body)
          )
        }
      ),

      this.client.subscribe(
        '/user/queue/chat',
        (message: IMessage) => {
          this.handlers.onChat?.(
            JSON.parse(message.body)
          )
        }
      ),

      this.client.subscribe(
        '/user/queue/errors',
        (message: IMessage) => {
          this.handlers.onError?.(
            JSON.parse(message.body)
          )
        }
      )
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

  private publish(
    destination: string,
    body: unknown
  ) {
    if (!this.client?.connected) {
      this.handlers.onError?.({
        code: 'NOT_CONNECTED',
        message:
          'Not connected to signaling server',
      })

      return
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body),
    })
  }

  isConnected(): boolean {
    return !!this.client?.connected
  }

  disconnect() {
    this.subs.forEach((subscription) => {
      subscription.unsubscribe()
    })

    this.subs = []

    this.client?.deactivate()

    this.client = null
  }
}

export const signalingClient =
  new SignalingClient()