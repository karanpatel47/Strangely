import type { CallState } from '../types'

const STATUS_TEXT: Record<CallState, string> = {
  idle: 'Idle',
  requesting_media: 'Requesting camera & microphone…',
  media_denied: 'Camera/microphone unavailable',
  connecting_ws: 'Connecting…',
  searching: 'Finding a stranger…',
  matched: 'Stranger found!',
  connecting_call: 'Connecting video…',
  connected: 'Connected',
  peer_left: 'Stranger disconnected',
  call_ended: 'Call ended',
  error: 'Something went wrong'
}

const DOT_COLOR: Record<CallState, string> = {
  idle: 'bg-textDim',
  requesting_media: 'bg-warn',
  media_denied: 'bg-danger',
  connecting_ws: 'bg-warn',
  searching: 'bg-warn',
  matched: 'bg-signal',
  connecting_call: 'bg-warn',
  connected: 'bg-signal',
  peer_left: 'bg-danger',
  call_ended: 'bg-textDim',
  error: 'bg-danger'
}

export function ConnectionStatus({ state }: { state: CallState }) {
  const pulsing = state === 'searching' || state === 'connecting_call' || state === 'connecting_ws'
  return (
    <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-panel border border-line w-fit">
      <span className={`relative h-2 w-2 rounded-full ${DOT_COLOR[state]} ${pulsing ? 'pulse-ring' : ''}`} />
      <span className="text-xs text-textDim font-medium tracking-wide">{STATUS_TEXT[state]}</span>
    </div>
  )
}
