import type { IceServerConfig } from '../types'

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

export async function fetchIceServers(): Promise<IceServerConfig> {
  const res = await fetch(`${API_URL}/config/ice-servers`)
  if (!res.ok) throw new Error('Failed to load ICE server config')
  return res.json()
}

export async function fetchActiveUserCount(): Promise<number> {
  const res = await fetch(`${API_URL}/stats/active-users`)
  if (!res.ok) throw new Error('Failed to load active user count')
  const data = (await res.json()) as { activeUsers: number }
  return data.activeUsers
}

export async function reportUser(reportedUserId: string, roomId: string, reason: string, reporterId: string) {
  await fetch(`${API_URL}/reports`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-User-Id': reporterId },
    body: JSON.stringify({ reportedUserId, roomId, reason })
  })
}
