/**
 * Attempts high-quality video first, then progressively relaxes constraints
 * if the device/browser can't satisfy them, instead of failing outright.
 */
export async function getLocalMedia(): Promise<MediaStream> {
  const attempts: MediaStreamConstraints[] = [
    { video: { width: { ideal: 1280 }, height: { ideal: 720 }, frameRate: { ideal: 30 } }, audio: true },
    { video: { width: { ideal: 640 }, height: { ideal: 480 } }, audio: true },
    { video: true, audio: true }
  ]

  let lastError: unknown = null
  for (const constraints of attempts) {
    try {
      return await navigator.mediaDevices.getUserMedia(constraints)
    } catch (err) {
      lastError = err
    }
  }
  throw lastError instanceof Error ? lastError : new Error('Could not access camera/microphone')
}

export function stopStream(stream: MediaStream | null | undefined) {
  stream?.getTracks().forEach((t) => t.stop())
}

export function classifyMediaError(err: unknown): 'permission_denied' | 'device_unavailable' | 'unknown' {
  const name = (err as { name?: string })?.name
  if (name === 'NotAllowedError' || name === 'SecurityError') return 'permission_denied'
  if (name === 'NotFoundError' || name === 'NotReadableError' || name === 'OverconstrainedError') return 'device_unavailable'
  return 'unknown'
}
