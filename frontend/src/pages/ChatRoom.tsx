import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { VideoPlayer } from '../components/VideoPlayer'
import { VideoControls } from '../components/VideoControls'
import { ChatPanel } from '../components/ChatPanel'
import { ConnectionStatus } from '../components/ConnectionStatus'
import { MatchmakingOverlay } from '../components/Matchmaking'
import { useMatchmaking } from '../hooks/useMatchmaking'

export default function ChatRoom() {
  const navigate = useNavigate()
  const {
    callState,
    localStream,
    remoteStream,
    chatLog,
    errorMessage,
    clearError,
    findNext,
    startSearchAgain,
    endCall,
    sendChatMessage
  } = useMatchmaking()

  const [toast, setToast] = useState<string | null>(null)

  useEffect(() => {
    if (errorMessage) {
      setToast(errorMessage)
      const t = setTimeout(() => {
        setToast(null)
        clearError()
      }, 5000)
      return () => clearTimeout(t)
    }
  }, [errorMessage, clearError])

  const chatActive = callState === 'connected' || callState === 'connecting_call'

  return (
    <div className="h-screen w-screen flex flex-col overflow-hidden bg-bg">
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
