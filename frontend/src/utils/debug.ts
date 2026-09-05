/**
 * Verbose [WebRTC]/[ChatRoom]/etc. tracing used throughout the signaling and
 * media code. Previously this was raw `console.log`, which ran (and paid
 * for string formatting + devtools overhead) on every ICE candidate and
 * signaling message in production too. `dlog` is a no-op unless the app is
 * running in dev mode or the person has explicitly turned on debug logging
 * via `localStorage.setItem('debug', 'true')`, matching the flag already
 * referenced in BLANK_PAGE_DEBUGGING.md.
 */
const debugEnabled =
  import.meta.env.DEV || (typeof localStorage !== 'undefined' && localStorage.getItem('debug') === 'true')

export function dlog(...args: unknown[]) {
  if (debugEnabled) {
    // eslint-disable-next-line no-console
    console.log(...args)
  }
}
