# Debugging Blank Page Issue - Troubleshooting Guide

## Issue
After selecting gender on Home page and clicking "Start Chat", the ChatRoom page appears blank instead of showing the video/chat interface.

---

## Quick Fixes to Try First

### 1. **Hard Refresh Browser**
Press `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows) to clear cache and reload. This ensures you have the latest code.

### 2. **Check Browser Console**
1. Press `F12` to open Developer Tools
2. Go to **Console** tab
3. Look for any error messages (red text)
4. Look for these log messages (green text starting with `[ChatRoom]`):
   - `[ChatRoom] location.state: { state: { gender: 'MALE' } }` ✅ Good
   - `[ChatRoom] no gender provided, redirecting to home` ❌ Problem: gender not passed

### 3. **Verify Backend is Running**
The blank page usually means the WebSocket can't connect to the backend.

```bash
# Start the backend (if not already running)
cd backend
mvn spring-boot:run
# Should show: "Started StrangerChatApplication in X seconds"
```

---

## Detailed Troubleshooting Steps

### Step 1: Check Console Logs

Open browser DevTools (`F12`) → **Console** tab and look for:

**✅ GOOD SIGNS:**
```
[ChatRoom] location.state: { state: { gender: 'MALE' } }
[ChatRoom] gender selected: MALE
[WebRTC] fetched ICE config: ...
```

**❌ PROBLEMS:**
```
[ChatRoom] no gender provided, redirecting to home
→ Gender was NOT passed from Home page

Could not reach the server. Check your connection and try again.
→ WebSocket connection failed

CORS error origin not allowed
→ Backend CORS configuration issue

Connection to stranger failed
→ WebSocket connected but matchmaking failed
```

---

### Step 2: Verify Gender Selection on Home Page

1. On Home page, ensure:
   - [ ] "Male" and "Female" buttons are visible
   - [ ] One button is highlighted in blue (selected)
   - [ ] "Start chatting" button is blue and clickable
   - [ ] "Start chatting" button is DISABLED (grey) when no gender selected

2. Check console after selecting gender:
   ```
   [Home] selectedGender: MALE  (should appear in console)
   ```

3. Click "Start chatting" and check console:
   ```
   navigate to /chat with state
   ```

---

### Step 3: Verify Navigation with Gender

After clicking "Start Chat", the console should show:

```
[ChatRoom] location.state: { state: { gender: 'MALE' } }
[ChatRoom] gender selected: MALE
```

**If you see "no gender provided" instead:**
- Gender is NOT being passed through React Router state
- Try clicking Back in browser and selecting gender again
- Or hard refresh the page (Cmd+Shift+R)

---

### Step 4: Check WebSocket Connection

In Console, look for connection logs:

**✅ GOOD:**
```
[WebRTC] fetched ICE config: [{ urls: [...] }]
→ Backend is responding
```

**❌ PROBLEMS:**
```
Could not reach the server. Check your connection and try again.
→ Backend is NOT running or CORS misconfigured
```

---

### Step 5: Verify Backend is Receiving Gender

Check backend logs:

```bash
# Terminal running backend should show:
[INFO] [WebSocket] User xxx connected
[DEBUG] find() called with gender: MALE
[DEBUG] Enqueuing user xxx for matching
```

If backend shows:
```
could not parse FindMatchRequest
→ Gender format is wrong (should be "MALE" or "FEMALE")

NullPointerException in find()
→ Gender is null (FindMatchRequest not properly received)
```

---

## Configuration Checklist

### Frontend Configuration
- [ ] `VITE_WS_URL` environment variable is set (or defaults to `http://localhost:8080/ws`)
- [ ] Frontend is served from correct port (usually 5173 for Vite dev server)
- [ ] Browser can access backend URL without CORS errors

### Backend Configuration
- [ ] Backend running on port 8080 (or correct port in config)
- [ ] Database connected (PostgreSQL running)
- [ ] Redis connected (if using Redis)
- [ ] Environment variables set:
  - `DATABASE_URL=jdbc:postgresql://localhost:5432/strangerdb`
  - `DATABASE_USERNAME=postgres`
  - `DATABASE_PASSWORD=...`
  - `REDIS_HOST=localhost`
  - `REDIS_PORT=6379`
  - `CORS_ALLOWED_ORIGINS=http://localhost:5173`

---

## Common Issues & Solutions

### Issue: "Connection Failed" Error After Gender Selection

**Cause:** Backend is not running or WebSocket URL is wrong

**Solution:**
```bash
# Terminal 1: Start backend
cd backend
mvn spring-boot:run

# Terminal 2: Check if it's running
curl -i http://localhost:8080/health
# Should return 200 OK

# Terminal 3: Start frontend (if using dev server)
cd frontend
npm run dev
# Open http://localhost:5173
```

---

### Issue: "Too many matchmaking requests" Error

**Cause:** Rate limiting kicked in (clicking buttons too fast)

**Solution:** Wait 10 seconds and try again

---

### Issue: Page Stuck on "Connecting..." Loading Spinner

**Cause:** WebSocket is trying to connect but not succeeding

**Check:**
1. Browser Console for errors
2. Backend logs for connection attempts
3. Network tab (F12 → Network) for WebSocket connection:
   - Should see `ws://localhost:8080/ws?userId=...`
   - Status should be `101 Switching Protocols` (WebSocket established)

---

### Issue: Gender Not Passing Through

**Cause:** React Router state lost during navigation

**Solution:**
1. Hard refresh (Cmd+Shift+R)
2. Check browser console for: `[ChatRoom] location.state:`
3. If null, verify Home.tsx is using: `navigate('/chat', { state: { gender: selectedGender } })`

---

## Network Debugging (Advanced)

Open DevTools → **Network** tab and filter for:

1. **WebSocket Connection:**
   - Name: `ws`
   - URL: `ws://localhost:8080/ws?userId=...`
   - Status: `101` (means it's open)
   - Should NOT show as failed/red

2. **Initial HTTP Requests:**
   - `/ws` → Status 101
   - `/config/ice` → Status 200 (should return ICE servers)
   - `/config/stun` → Status 200 (if applicable)

---

## Testing Checklist

### Frontend (Without Backend)
- [ ] Home page loads
- [ ] Gender selection works (buttons highlight)
- [ ] Start Chat button disables/enables correctly
- [ ] Can navigate to /chat and see "Connecting..." loading state

### With Backend Running
- [ ] WebSocket connects (see status 101 in Network tab)
- [ ] See "Connecting..." → then camera/video shows
- [ ] Backend logs show user connected and gender received
- [ ] Can see "Waiting for stranger..." or match happens

### End-to-End (Two Browsers/Users)
- [ ] Browser 1: Select Male, click Start Chat
- [ ] Browser 2: Select Female, click Start Chat
- [ ] Both should match and see each other's video
- [ ] Database should record both genders in sessions table

---

## Manual Database Check

After two users connect, check if genders are stored:

```sql
-- PostgreSQL
SELECT id, gender FROM users LIMIT 5;
SELECT id, user_a_gender, user_b_gender FROM sessions LIMIT 5;
```

Should show:
```
 id                                 | gender
------------------------------------+--------
 550e8400-e29b-41d4-a716-446655440000 | MALE
 6ba7b810-9abd-41d4-853b-cde789800002 | FEMALE

 id                                 | user_a_gender | user_b_gender
------------------------------------+---------------+---------------
 550e8400-e29b-41d4-a716-446655440001 | MALE          | FEMALE
```

---

## Debug Mode: Enable Console Logs

Add this to browser Console (F12) to enable extra logging:

```javascript
// Enable debug logging
localStorage.setItem('debug', 'true')
// Reload page
location.reload()
```

---

## Still Blank? Try These Steps

1. **Check All Tabs in DevTools:**
   - Console: Any errors?
   - Network: WebSocket connected?
   - Application: localStorage has user-id?

2. **Backend Logs:**
   - Are you seeing user connection messages?
   - Any exceptions related to FindMatchRequest?

3. **Browser Cache:**
   - Hard refresh with Cmd+Shift+R
   - Or Clear Cache from DevTools (Settings → Clear site data)

4. **Network Issues:**
   - Firewall blocking WebSocket?
   - VPN active that might block localhost?
   - Try accessing backend: `curl http://localhost:8080/health`

5. **Database:**
   - Is PostgreSQL running?
   - Check backend logs for connection errors

---

## Report a Bug

If still not working, collect this info and report:

1. Browser: Chrome, Firefox, Safari?
2. Backend logs (full output):
   ```bash
   mvn spring-boot:run 2>&1 | head -100
   ```
3. Frontend Console (F12 → Console, full output)
4. Network tab showing WebSocket attempt
5. Does `curl http://localhost:8080/health` work?

