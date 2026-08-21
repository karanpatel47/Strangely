interface Props {
  onNext: () => void
  onEnd: () => void
  disabled?: boolean
}

export function VideoControls({ onNext, onEnd, disabled }: Props) {
  return (
    <div className="flex items-left justify-left gap-3 py-1">
      <button
        onClick={onNext}
        disabled={disabled}
        className="h-10 px-6 rounded-full bg-signal text-ink font-display font-semibold text-sm tracking-wide hover:opacity-90 disabled:opacity-40 disabled:cursor-not-allowed transition shadow-md"
      >
        Next ⏭
      </button>

      <button
        onClick={onEnd}
        className="h-10 px-6 rounded-full bg-panel2 border border-line text-textDim hover:text-danger hover:border-danger/50 font-display text-sm tracking-wide transition"
      >
        End call
      </button>
    </div>
  )
}

