// These mirror com.strangerchat.dto.* exactly - field names and enum values
// must match the backend or STOMP (de)serialization will silently break.

export type SignalType = 'OFFER' | 'ANSWER' | 'ICE_CANDIDATE'

export interface SignalMessage {
  roomId: string
  type: SignalType
  payload: string
}

export interface ChatMessageDto {
  roomId: string
  content: string
  senderId?: string
  timestamp?: number
}

export type RoomEventType = 'MATCH_FOUND' | 'PEER_LEFT' | 'PEER_NEXT' | 'CALL_ENDED' | 'WAITING'

export interface RoomEventMessage {
  type: RoomEventType
  roomId: string | null
  peerId: string | null
  initiator: boolean
}

export interface ErrorMessage {
  code: string
  message: string
}

export interface IceServer {
  urls: string[]
  username?: string
  credential?: string
}

export interface IceServerConfig {
  iceServers: IceServer[]
}

export type CallState =
  | 'idle'
  | 'requesting_media'
  | 'media_denied'
  | 'connecting_ws'
  | 'searching'
  | 'matched'
  | 'connecting_call'
  | 'connected'
  | 'peer_left'
  | 'call_ended'
  | 'error'

export interface ChatEntry {
  id: string
  sender: 'me' | 'stranger'
  content: string
  timestamp: number
}
