import type { CallState } from '../types'

interface Props {
  state: CallState
  onSearchAgain: () => void
}

export function MatchmakingOverlay({ state, onSearchAgain }: Props) {
  if (state === 'connected') return null

  const copy: Partial<Record<CallState, { title: string; subtitle: string }>> = {
    searching: { title: 'Finding a stranger…', subtitle: 'This usually takes a few seconds' },
    connecting_call: { title: 'Stranger found!', subtitle: 'Setting up video…' },
    peer_left: { title: 'Stranger disconnected', subtitle: 'They left the conversation' },
    call_ended: { title: 'Call ended', subtitle: 'Ready when you are' },
    media_denied: { title: 'Camera unavailable', subtitle: 'Check your browser permissions and reload' }
  }

  const info = copy[state]
  if (!info) return null

  return (
    <div className="absolute inset-0 flex flex-col items-center justify-center gap-4 bg-ink/80 backdrop-blur-sm rounded-2xl fade-in">
      {(state === 'searching' || state === 'connecting_call') && (
        <div className="h-10 w-10 rounded-full border-2 border-signal/30 border-t-signal animate-spin" />
      )}
      <div className="text-center">
        <p className="font-display font-semibold text-lg">{info.title}</p>
        <p className="text-sm text-textDim mt-1">{info.subtitle}</p>
      </div>
      {(state === 'peer_left' || state === 'call_ended') && (
        <button
          onClick={onSearchAgain}
          className="mt-2 px-5 py-2 rounded-full bg-signal text-ink text-sm font-display font-semibold hover:opacity-90 transition"
        >
          Find another stranger
        </button>
      )}
    </div>
  )
}
