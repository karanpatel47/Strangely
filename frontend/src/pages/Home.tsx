import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Gender } from '../types'

export default function Home() {
  const navigate = useNavigate()
  const [selectedGender, setSelectedGender] = useState<Gender | null>(null)

  const handleStartChat = () => {
    if (selectedGender) {
      navigate('/chat', { state: { gender: selectedGender } })
    }
  }

  return (
    <div className="relative min-h-screen flex flex-col overflow-hidden">
      {/* Ambient background: two soft, slowly-drifting glows. Purely
          decorative — kept as fixed absolutely-positioned blurred circles so
          they never affect layout or scroll. */}
      <div aria-hidden className="pointer-events-none absolute inset-0 overflow-hidden">
        <div className="absolute -top-32 -left-24 h-80 w-80 rounded-full bg-signal/10 blur-[100px] animate-drift-slow" />
        <div className="absolute bottom-[-6rem] right-[-4rem] h-96 w-96 rounded-full bg-signal/[0.06] blur-[120px] animate-drift-slower" />
      </div>

      <header className="relative px-6 py-5 flex items-center justify-between">
        <span className="font-display font-bold tracking-tight text-lg">Strangely</span>
        <span className="text-xs text-textDim">18+ · No account needed</span>
      </header>

      <main className="relative flex-1 flex flex-col items-center justify-center px-6 text-center gap-8">
        <div className="max-w-xl flex flex-col items-center">
          <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full bg-signal/10 border border-signal/30 text-xs font-semibold text-signal tracking-wide mb-4 shadow-sm">
            <span className="h-2 w-2 rounded-full bg-signal animate-pulse" />
            <span>Meet &bull; Connect &bull; Share</span>
          </div>
          <h1 className="font-display text-4xl sm:text-5xl font-bold leading-tight">
            Talk to someone <span className="text-signal">new</span>, right now.
          </h1>
          <p className="mt-4 text-textDim text-base sm:text-lg leading-relaxed">
            Connect with new people, exchange knowledge, and share your skills in a respectful community.
          </p>
        </div>

        <div className="flex flex-col items-center gap-6 w-full max-w-xs">
          <div className="w-full">
            <label className="block text-sm font-semibold text-text mb-3">How do you identify?</label>
            <div className="flex gap-3 justify-center" role="radiogroup" aria-label="Gender">
              <button
                role="radio"
                aria-checked={selectedGender === 'MALE'}
                onClick={() => setSelectedGender('MALE')}
                className={`px-6 py-3 rounded-lg font-semibold transition-all duration-150 ${
                  selectedGender === 'MALE'
                    ? 'bg-signal text-ink scale-[1.03] shadow-lg shadow-signal/20'
                    : 'bg-panel2 text-text border border-line hover:border-signal/60 hover:-translate-y-0.5'
                }`}
              >
                Male
              </button>
              <button
                role="radio"
                aria-checked={selectedGender === 'FEMALE'}
                onClick={() => setSelectedGender('FEMALE')}
                className={`px-6 py-3 rounded-lg font-semibold transition-all duration-150 ${
                  selectedGender === 'FEMALE'
                    ? 'bg-signal text-ink scale-[1.03] shadow-lg shadow-signal/20'
                    : 'bg-panel2 text-text border border-line hover:border-signal/60 hover:-translate-y-0.5'
                }`}
              >
                Female
              </button>
            </div>
          </div>

          <button
            onClick={handleStartChat}
            disabled={selectedGender === null}
            className={`w-full px-8 py-4 rounded-full font-display font-semibold text-base tracking-wide transition-all duration-150 ${
              selectedGender === null
                ? 'bg-textDim/20 text-textDim cursor-not-allowed'
                : 'bg-signal text-ink hover:opacity-90 hover:shadow-xl hover:shadow-signal/20 active:scale-95'
            }`}
          >
            Start chatting
          </button>
        </div>

        <div className="flex flex-wrap justify-center gap-x-6 gap-y-2 text-xs text-textDim max-w-md">
          <span>🔒 Video never touches our servers — it streams peer-to-peer</span>
          <span>🚫 No recording, no accounts, nothing stored except abuse reports</span>
        </div>
      </main>

      <footer className="relative px-6 py-4 text-center text-[11px] text-textDim/60">
        By continuing you agree to be respectful. Report abusive strangers using the flag button in-call.
      </footer>
    </div>
  )
}
