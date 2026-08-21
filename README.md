# Nearcast — Stranger Video Chat (Phase 1 MVP)

Random 1-to-1 stranger video/audio/text chat. Video and audio stream **peer-to-peer over WebRTC** — the backend only handles matchmaking, signaling, and chat relay, and never touches media.

```
Browser A  ⇄ (WebRTC media: video/audio) ⇄  Browser B
     │                                            │
     └──────────── WebSocket/STOMP ───────────────┘
                        │
                 Spring Boot backend
                (matchmaking, signaling,
                 chat relay, presence)
                        │
                     Redis
              (queue, rooms, presence)
                        │
                   PostgreSQL
           (users, sessions, reports — metadata only)
```

## Project layout

```
stranger-chat/
├── backend/          Spring Boot 3 / Java 21 — matchmaking, signaling, chat, REST
├── frontend/          React + Vite + TS + Tailwind — WebRTC client, UI
├── infra/coturn/       coturn TURN server config
├── docker-compose.yml
└── .env.example
```

## Quick start (Docker)

```bash
cp .env.example .env
# edit .env if needed (defaults work for local dev)
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api/health
- Postgres: localhost:5432
- Redis: localhost:6379
- coturn: UDP/TCP 3478, TLS 5349

Open http://localhost:5173 in **two different browser windows/profiles** (or two devices), click **Start chatting** in both, allow camera/mic, and they'll be matched with each other.

## Running without Docker

**Backend** (needs local Postgres + Redis running, or use `docker compose up postgres redis`):

```bash
cd backend
export DATABASE_URL=jdbc:postgresql://localhost:5432/strangerchat
export DATABASE_USERNAME=strangerchat
export DATABASE_PASSWORD=strangerchat
export REDIS_HOST=localhost
export REDIS_PORT=6379
export CORS_ALLOWED_ORIGINS=http://localhost:5173
./mvnw spring-boot:run
```

**Frontend:**

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

## WebSocket/STOMP protocol

Connect to `ws://localhost:8080/ws?userId=<id>` (SockJS-wrapped). `userId` is a client-generated UUID persisted in `localStorage` so a page refresh reconnects as "the same" anonymous user.

**Client → server (`/app/...`):**

| Destination | Payload | Purpose |
|---|---|---|
| `/app/match/find` | — | Enter the matchmaking queue |
| `/app/match/next` | — | Leave current room, immediately re-queue |
| `/app/call/offer` | `SignalMessage{roomId,type:OFFER,payload:sdp}` | Relay SDP offer to peer |
| `/app/call/answer` | `SignalMessage{roomId,type:ANSWER,payload:sdp}` | Relay SDP answer to peer |
| `/app/call/ice` | `SignalMessage{roomId,type:ICE_CANDIDATE,payload:json}` | Relay ICE candidate to peer |
| `/app/chat/send` | `ChatMessageDto{roomId,content}` | Send a chat message |
| `/app/call/end` | — | End the call, leave the room |

**Server → client (subscriptions):**

| Destination | Payload | Purpose |
|---|---|---|
| `/user/queue/room` | `RoomEventMessage` | `MATCH_FOUND`, `PEER_LEFT`, `PEER_NEXT`, `CALL_ENDED`, `WAITING` |
| `/user/queue/signal` | `SignalMessage` | Forwarded SDP/ICE from peer |
| `/user/queue/chat` | `ChatMessageDto` | Chat messages (including echo of your own, server-timestamped) |
| `/user/queue/errors` | `ErrorMessage` | Validation, rate-limit, or room-membership errors |

## Matchmaking flow

1. Client connects, sends `/app/match/find`.
2. Backend pops the oldest **still-present** user off the Redis `matchmaking:waiting` list.
   - If none: the caller is pushed onto the queue and gets a `WAITING` event.
   - If found: a `roomId` (UUID) is created, both users are marked as occupying that room in Redis, and both get `MATCH_FOUND` — the user who was already waiting is designated the SDP **offer initiator** (avoids both sides racing to send an offer).
3. Session start is recorded asynchronously to Postgres.

Race conditions handled: concurrent `find` calls rely on Redis's atomic `LPOP` (only one caller can ever pop a given waiting user); a user can't match with themself (self-entries are skipped); stale queue entries from users who disconnected while waiting are filtered via the `presence:{userId}` TTL key; repeated `Next` clicks are idempotent because `leaveRoom`/`removeFromQueue` are no-ops if there's nothing to clean up.

## WebRTC connection flow

1. On `MATCH_FOUND`, both clients create an `RTCPeerConnection` with ICE servers fetched from `/api/config/ice-servers` (STUN always, TURN if configured) and attach their local media tracks.
2. The **initiator** calls `createOffer()`, sets it as local description, and sends it via `/app/call/offer`.
3. The backend relays it to the peer's `/user/queue/signal`. The peer calls `createAnswer()` and sends it back via `/app/call/answer`.
4. Both sides exchange ICE candidates as they're discovered via `/app/call/ice`; candidates that arrive before the remote description is set are buffered and flushed afterward.
5. Once ICE completes, media flows **directly between browsers**. If `iceConnectionState` becomes `failed` (e.g. network change), the client calls `restartIce()` once before surfacing an error.

## Next / End Call

- **Next**: closes the local `RTCPeerConnection`, tells the backend (`/app/match/next`), which tears down the Redis room, notifies the peer (`PEER_NEXT`), and immediately re-queues the caller — no page reload.
- **End Call**: same teardown via `/app/call/end`, but does **not** re-queue; the UI shows "Call ended" with a "Find another stranger" button.
- **Ungraceful disconnect** (browser closed, tab killed, network drop): Spring's `SessionDisconnectEvent` fires server-side even without an explicit message, so the peer is still notified (`PEER_LEFT`) and Redis state is cleaned up.

## Redis data model

| Key | Type | Purpose |
|---|---|---|
| `matchmaking:waiting` | List | FIFO queue of userIds waiting for a match |
| `matchmaking:waiting:set` | Set | Mirror of the same ids for O(1) "am I queued" checks |
| `room:{roomId}` | Hash | `{userA, userB, createdAt}` — active room registry |
| `user:room:{userId}` | String | userId → roomId, O(1) lookup of "what room am I in" |
| `presence:{userId}` | String (TTL) | Heartbeat; expiry signals an ungraceful disconnect |
| `ratelimit:{bucket}` | String (TTL counter) | Fixed-window rate limiting for chat/matchmaking |

A List (not a Set) is used for the waiting queue specifically to preserve FIFO fairness — the person who's been waiting longest gets matched first.

## PostgreSQL schema

- `users` — anonymous userId, first/last seen, banned flag
- `sessions` — roomId, both participant ids, start/end time, end reason (`NEXT`/`END_CALL`/`DISCONNECT`)
- `reports` — abuse reports (reporter, reported user, room, reason)

No video/audio content is ever persisted — only session metadata for moderation/analytics.

## Error handling

- Camera/mic permission denied or device unavailable → clear in-app message, "media_denied" state, retry via page reload.
- WebSocket unreachable → toast + `wsStatus: 'failed'`.
- Stranger disconnects/Next → overlay with "Find another stranger" button, no dead-end loading state.
- ICE failure → one automatic `restartIce()` attempt before surfacing an error.
- Chat flooding / rapid matchmaking retries → Redis-backed rate limiting returns a `RATE_LIMITED` error over `/user/queue/errors`.
- Oversized/malformed STOMP frames → caught by `StompErrorHandler`, session isn't killed silently.

## Common errors & fixes

| Symptom | Likely cause | Fix |
|---|---|---|
| "Could not reach the server" toast | Backend not running / wrong `VITE_WS_URL` | Check `docker compose ps`, confirm backend is healthy on 8080 |
| Camera preview black, no error | Browser blocked autoplay or wrong device selected | Check OS-level camera permissions for the browser |
| Never gets matched (2 tabs) | Both tabs share the same `localStorage` userId (same browser profile) | Use two separate browser profiles/incognito windows, or two devices |
| Video connects but remote is frozen | TURN not reachable behind strict NAT and only STUN configured | Ensure `TURN_SERVER`/coturn is running and ports 3478 + 49160–49200 are open |
| 403/CORS errors in console | `CORS_ALLOWED_ORIGINS` doesn't match the frontend's actual origin | Update `.env` and restart backend |

## Testing the complete system locally

1. `docker compose up --build`
2. Open two browser windows (different profiles) to http://localhost:5173
3. Click **Start chatting** in both, allow camera/mic in both
4. Confirm both see each other's video within a few seconds, chat messages appear on both sides instantly
5. Click **Next** on one side — confirm the other side sees "Stranger disconnected" and can click "Find another stranger"; the Next side automatically re-enters the queue
6. Click **End call** — confirm the local peer connection closes and the peer is notified
7. Close one browser tab entirely mid-call — confirm the other side is still notified ("Stranger disconnected") within a couple seconds
8. Toggle mic/camera buttons — confirm state indicator and peer's audio/video visibly change

## Security notes (Phase 1 scope)

- Anonymous per-connection identity (no accounts) — a userId is a UUID, not tied to any PII.
- CORS locked to configured origins; WebSocket handshake validated per-connection.
- Server-side room-membership checks before relaying any signaling or chat message.
- Rate limiting on matchmaking and chat.
- Message length capped (1000 chars), control characters stripped.
- STUN/TURN credentials served from backend config, never hardcoded in the frontend bundle.
- No media ever transits or is stored on the backend.

This is a Phase 1 MVP: production hardening (TURN credential rotation, structured moderation/ban enforcement, horizontal scaling of the STOMP broker via a real message broker like RabbitMQ, HTTPS/WSS termination, etc.) is out of scope here but the architecture is designed to accommodate it later.
