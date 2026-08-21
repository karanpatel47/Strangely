import { useEffect, useRef, useState } from 'react'
import type { ChatEntry } from '../types'

interface Props {
  messages: ChatEntry[]
  onSend: (content: string) => void
  disabled?: boolean
}

export function ChatPanel({ messages, onSend, disabled }: Props) {
  const [draft, setDraft] = useState('')
  const scrollRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages])

  function submit() {
    const trimmed = draft.trim()
    if (!trimmed || disabled) return
    onSend(trimmed)
    setDraft('')
  }

  return (
    <div className="flex flex-col h-full bg-panel border border-line rounded-2xl overflow-hidden">
      <div className="px-4 py-1 border-b border-line">
        <h3 className="font-display text-sm font-semibold tracking-wide text-textDim">CHAT</h3>
      </div>

      <div ref={scrollRef} className="flex-1 overflow-y-auto px-4 py-3 space-y-3">
        {messages.length === 0 && (
          <p className="text-sm text-textDim/70 italic">Say hi — messages appear here for both of you.</p>
        )}
        {messages.map((m) => (
          <div key={m.id} className="fade-in">
            <div className={`flex ${m.sender === 'me' ? 'justify-end' : 'justify-start'}`}>
              <div
                className={`max-w-[80%] rounded-xl px-3 py-2 text-sm ${m.sender === 'me'
                  ? 'bg-signalDim text-text border border-signal/30'
                  : 'bg-panel2 text-text border border-line'
                  }`}
              >
                <span className="block text-[10px] uppercase tracking-wide text-textDim mb-0.5">
                  {m.sender === 'me' ? 'You' : 'Stranger'} · {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </span>
                {m.content}
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="p-3 border-t border-line flex gap-2">
        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') submit()
          }}
          disabled={disabled}
          placeholder={disabled ? 'Waiting for a connection…' : 'Write a message…'}
          maxLength={1000}
          className="flex-1 bg-panel2 border border-line rounded-lg px-3 py-2 text-sm placeholder:text-textDim/60 focus:outline-none focus:border-signal/60 disabled:opacity-50"
        />
        <button
          onClick={submit}
          disabled={disabled || !draft.trim()}
          className="px-4 rounded-lg bg-signal text-ink text-sm font-semibold disabled:opacity-30 disabled:cursor-not-allowed hover:opacity-90 transition"
        >
          Send
        </button>
      </div>
    </div>
  )
}
