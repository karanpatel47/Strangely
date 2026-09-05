import { useEffect, useState } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { VideoPlayer } from '../components/VideoPlayer'
import { VideoControls } from '../components/VideoControls'
import { ChatPanel } from '../components/ChatPanel'
import { ConnectionStatus } from '../components/ConnectionStatus'
import { MatchmakingOverlay } from '../components/Matchmaking'
import { useMatchmaking } from '../hooks/useMatchmaking'
import type { Gender } from '../types'

export default function ChatRoom() {
  const navigate = useNavigate()
  const location = useLocation()

  // Read gender synchronously from router state on the very first render
  // (via the useState initializer) instead of setting it in an effect.
  // Previously this component mounted with gender=null, called
  // useMatchmaking({ gender: null }) on the first render, which kicked off
  // a real getUserMedia() prompt and a WebSocket connection before the
  // "no gender, redirect home" check even ran — then a second render with
  // the real gender tore that down and reconnected everything again. Doing
  // it this way means gender is correct (or null) from the very first
  // render and never changes afterwards, so downstream hooks only ever do
  // real work once.
  const [gender] = useState<Gender | null>(() => (location.state as { gender?: Gender } | null)?.gender ?? null)
  const [toast, setToast] = useState<string | null>(null)

  // ✅ CRITICAL: All hooks must be called at the top level, BEFORE any conditional returns
  const matchmakingResult = useMatchmaking({ gender })

  useEffect(() => {
    if (!gender) {
      navigate('/', { replace: true })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (matchmakingResult?.errorMessage) {
      setToast(matchmakingResult.errorMessage)
      const t = setTimeout(() => {
        setToast(null)
        if (matchmakingResult?.clearError) {
          matchmakingResult.clearError()
        }
      }, 5000)
      return () => clearTimeout(t)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [matchmakingResult?.errorMessage])

  // Now conditional returns are OK since all hooks are already called
  if (!gender) {
    return (
      <div className="h-screen w-screen flex items-center justify-center bg-ink">
        <div className="text-center">
          <div className="mb-4 w-12 h-12 border-4 border-signal/30 border-t-signal rounded-full animate-spin mx-auto"></div>
          <p className="text-textDim">Redirecting…</p>
        </div>
      </div>
    )
  }

  const {
    callState,
    localStream,
    remoteStream,
    chatLog,
    findNext,
    startSearchAgain,
    endCall,
    sendChatMessage
  } = matchmakingResult

  // Show error state if WebSocket fails
  if (callState === 'error' || (callState === 'connecting_ws' && toast?.includes('Could not reach'))) {
    return (
      <div className="h-screen w-screen flex flex-col items-center justify-center bg-ink p-6">
        <div className="max-w-md text-center">
          <div className="mb-4 text-4xl">⚠️</div>
          <h2 className="text-xl font-bold text-text mb-2">Connection Failed</h2>
          <p className="text-textDim mb-6">{toast || 'Could not connect to the server. Make sure it is running and try again.'}</p>
          <button
            onClick={() => {
              setToast(null)
              navigate('/')
            }}
            className="px-6 py-2 bg-signal text-ink rounded-lg font-semibold hover:opacity-90"
          >
            Back to Home
          </button>
        </div>
      </div>
    )
  }

  const chatActive = callState === 'connected' || callState === 'connecting_call'

  return (
    <div className="h-screen w-screen flex flex-col overflow-hidden bg-ink">
      <header className="px-4 py-1 flex items-center justify-between border-b border-line shrink-0">
        <button onClick={() => navigate('/')} className="font-display font-bold tracking-tight text-lg">
          Strangely
        </button>
        <ConnectionStatus state={callState} />
      </header>

      {toast && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 bg-panel2 border border-danger/40 text-sm px-4 py-2 rounded-lg fade-in shadow-lg">
          {toast}
        </div>
      )}

      <main className="flex-1 flex flex-col p-3 gap-2 overflow-hidden min-h-0">
        <div className="flex-1 grid grid-cols-1 sm:grid-cols-[minmax(0,1.1fr)_minmax(0,1fr)] gap-2 min-h-0 relative">
          <div className="relative h-full w-full overflow-hidden rounded-xl">
            <VideoPlayer stream={remoteStream} label="Stranger" className="h-full w-full" />
            <MatchmakingOverlay state={callState} onSearchAgain={startSearchAgain} />
          </div>

          <div className="relative h-full w-full overflow-hidden rounded-xl">
            <VideoPlayer stream={localStream} muted mirrored label="You" className="h-full w-full" />
          </div>
        </div>

        <div className="shrink-0">
          <VideoControls
            onNext={findNext}
            onEnd={endCall}
            disabled={callState === 'media_denied' || callState === 'connecting_ws'}
          />
        </div>

        <div className="h-44 sm:h-52 shrink-0 w-full">
          <ChatPanel messages={chatLog} onSend={sendChatMessage} disabled={!chatActive} />
        </div>
      </main>
    </div>
  )
}
