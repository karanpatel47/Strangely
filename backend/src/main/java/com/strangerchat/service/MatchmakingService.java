package com.strangerchat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis keys used:
 *
 *   matchmaking:waiting        (LIST)   FIFO queue of userIds waiting for a match
 *   matchmaking:waiting:set    (SET)    mirror of the same ids, for O(1) membership checks
 *   room:{roomId}              (HASH)   {userA, userB, createdAt} - active room registry
 *   user:room:{userId}         (STRING) userId -> roomId, for O(1) "what room am I in"
 *   presence:{userId}          (STRING) heartbeat key with TTL, expiry = ungraceful disconnect
 *
 * Concurrency notes:
 *  - Enqueue/dequeue relies on Redis's single-threaded command execution: LPOP and
 *    RPUSH are each atomic, so two users calling find() at "the same time" can never
 *    both pop the same waiting user - only one caller's LPOP will see it.
 *  - We still guard against self-matching (a user's own id at the head of the queue,
 *    which can happen if they call Next twice quickly before cleanup completes).
 *  - Room teardown (leaveRoom) is idempotent: if the room hash is already gone,
 *    it's a no-op rather than an error, so a duplicate Next/disconnect never throws.
 */
@Service
public class MatchmakingService {

    private static final Logger log = LoggerFactory.getLogger(MatchmakingService.class);

    private static final String WAITING_LIST = "matchmaking:waiting";
    private static final String WAITING_SET = "matchmaking:waiting:set";
    private static final String ROOM_PREFIX = "room:";
    private static final String USER_ROOM_PREFIX = "user:room:";
    private static final String PRESENCE_PREFIX = "presence:";

    private final StringRedisTemplate redis;

    @Value("${app.matchmaking.presence-ttl-seconds:30}")
    private long presenceTtlSeconds;

    public MatchmakingService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public record Match(String roomId, String peerId, boolean initiator) {}

    /**
     * Attempts to match the given user with someone already waiting.
     * Returns a Match if paired immediately, or empty if the user was
     * added to the queue and must wait for someone else to arrive.
     */
    public Optional<Match> findMatch(String userId) {
        touchPresence(userId);

        // Defensive cleanup: never allow a user to be in the queue twice.
        removeFromQueue(userId);

        String peerId = null;
        // Pop candidates until we find one that isn't the same user and is still present.
        for (int attempts = 0; attempts < 5; attempts++) {
            String candidate = redis.opsForList().leftPop(WAITING_LIST);
            if (candidate == null) {
                break; // queue empty
            }
            redis.opsForSet().remove(WAITING_SET, candidate);

            if (candidate.equals(userId)) {
                continue; // stale self-entry, skip
            }
            if (!isPresent(candidate)) {
                log.debug("Skipping stale waiting user {}", candidate);
                continue; // they disconnected while queued
            }
            peerId = candidate;
            break;
        }

        if (peerId == null) {
            enqueue(userId);
            return Optional.empty();
        }

        String roomId = UUID.randomUUID().toString();
        createRoom(roomId, userId, peerId);
        // The user who was already waiting (peerId) becomes the SDP offer initiator,
        // arbitrary but deterministic so both sides agree without extra negotiation.
        return Optional.of(new Match(roomId, peerId, false));
    }

    /** Companion lookup used by the caller to know who the newly-matched peer is from *their* side. */
    public boolean peerIsInitiator() {
        return true;
    }

    private void enqueue(String userId) {
        redis.opsForList().rightPush(WAITING_LIST, userId);
        redis.opsForSet().add(WAITING_SET, userId);
    }

    public void removeFromQueue(String userId) {
        redis.opsForList().remove(WAITING_LIST, 0, userId);
        redis.opsForSet().remove(WAITING_SET, userId);
    }

    public boolean isQueued(String userId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(WAITING_SET, userId));
    }

    private void createRoom(String roomId, String userA, String userB) {
        Map<String, String> room = Map.of(
                "userA", userA,
                "userB", userB,
                "createdAt", Instant.now().toString()
        );
        redis.opsForHash().putAll(ROOM_PREFIX + roomId, room);
        redis.opsForValue().set(USER_ROOM_PREFIX + userA, roomId);
        redis.opsForValue().set(USER_ROOM_PREFIX + userB, roomId);
    }

    public Optional<String> getCurrentRoom(String userId) {
        String roomId = redis.opsForValue().get(USER_ROOM_PREFIX + userId);
        return Optional.ofNullable(roomId);
    }

    public Optional<String> getPeer(String roomId, String userId) {
        Object userA = redis.opsForHash().get(ROOM_PREFIX + roomId, "userA");
        Object userB = redis.opsForHash().get(ROOM_PREFIX + roomId, "userB");
        if (userA == null || userB == null) return Optional.empty();
        if (userId.equals(userA.toString())) return Optional.of(userB.toString());
        if (userId.equals(userB.toString())) return Optional.of(userA.toString());
        return Optional.empty();
    }

    public boolean roomExists(String roomId) {
        return Boolean.TRUE.equals(redis.hasKey(ROOM_PREFIX + roomId));
    }

    /** Tears down a room and clears both participants' user->room pointer. Idempotent. */
    public void leaveRoom(String roomId, String userId) {
        Optional<String> peer = getPeer(roomId, userId);
        redis.delete(ROOM_PREFIX + roomId);
        redis.delete(USER_ROOM_PREFIX + userId);
        peer.ifPresent(p -> redis.delete(USER_ROOM_PREFIX + p));
    }

    public void touchPresence(String userId) {
        redis.opsForValue().set(PRESENCE_PREFIX + userId, "1", Duration.ofSeconds(presenceTtlSeconds));
    }

    public boolean isPresent(String userId) {
        return Boolean.TRUE.equals(redis.hasKey(PRESENCE_PREFIX + userId));
    }

    public void clearPresence(String userId) {
        redis.delete(PRESENCE_PREFIX + userId);
    }
}
