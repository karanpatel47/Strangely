import { useNavigate } from 'react-router-dom'

export default function Home() {
  const navigate = useNavigate()

  return (
    <div className="min-h-screen flex flex-col">
      <header className="px-6 py-5 flex items-center justify-between">
        <span className="font-display font-bold tracking-tight text-lg">Strangely</span>
        <span className="text-xs text-textDim">18+ · No account needed</span>
      </header>

      <main className="flex-1 flex flex-col items-center justify-center px-6 text-center gap-8">
        <div className="max-w-xl">
          <h1 className="font-display text-4xl sm:text-5xl font-bold leading-tight">
            Talk to someone <span className="text-signal">new</span>, right now.
          </h1>
          <p className="mt-4 text-textDim text-base sm:text-lg">
            Strangely pairs you with a random stranger for live video, audio, and text —
            no sign-up, no profiles, just a conversation. Click next any time to meet someone else.
          </p>
        </div>

        <button
          onClick={() => navigate('/chat')}
          className="px-8 py-4 rounded-full bg-signal text-ink font-display font-semibold text-base tracking-wide hover:opacity-90 active:scale-95 transition"
        >
          Start chatting
        </button>

        <div className="flex flex-wrap justify-center gap-4 text-xs text-textDim max-w-md">
          <span>🔒 Video never touches our servers — it streams peer-to-peer</span>
          <span>🚫 No recording, no accounts, nothing stored except abuse reports</span>
        </div>
      </main>

      <footer className="px-6 py-4 text-center text-[11px] text-textDim/60">
        By continuing you agree to be respectful. Report abusive strangers using the flag button in-call.
      </footer>
    </div>
  )
}
