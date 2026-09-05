import { useEffect, useState } from 'react'
import { fetchActiveUserCount } from '../services/api'

/**
 * Polls the (already-existing) /api/stats/active-users endpoint so the
 * homepage can show a live "X people online now" badge. Failures are
 * swallowed on purpose — this is a nice-to-have, not something that should
 * ever block or error out the page if the stats endpoint is briefly
 * unreachable.
 */
export function useActiveUserCount(pollMs = 8000): number | null {
  const [count, setCount] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false

    async function tick() {
      try {
        const value = await fetchActiveUserCount()
        if (!cancelled) setCount(value)
      } catch {
        // Ignore — keep showing the last known value.
      }
    }

    tick()
    const id = setInterval(tick, pollMs)
    return () => {
      cancelled = true
      clearInterval(id)
    }
  }, [pollMs])

  return count
}
