import { useEffect, useRef } from 'react'

interface Props {
  stream: MediaStream | null
  muted?: boolean
  mirrored?: boolean
  label?: string
  placeholderText?: string
  showPlaceholder?: boolean
  className?: string
}

export function VideoPlayer({ stream, muted, mirrored, label, placeholderText, showPlaceholder, className }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null)

  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.srcObject = stream
    }
  }, [stream])

  return (
    <div className={`relative overflow-hidden rounded-1xl bg-panel border border-line ${className ?? ''}`}>
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted={muted}
        className={`h-full w-full object-cover ${mirrored ? 'scale-x-[-1]' : ''} ${showPlaceholder ? 'opacity-0' : 'opacity-100'} transition-opacity duration-300`}
      />
      {showPlaceholder && (
        <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 bg-panel2">
          <div className="h-14 w-14 rounded-full bg-line flex items-center justify-center text-2xl">🚫📷</div>
          <p className="text-sm text-textDim">{placeholderText ?? 'Camera off'}</p>
        </div>
      )}
      {label && (
        <span className="absolute bottom-2 left-2 px-2 py-1 rounded-md bg-ink/70 text-xs text-textDim tracking-wide backdrop-blur-sm">
          {label}
        </span>
      )}
    </div>
  )
}
