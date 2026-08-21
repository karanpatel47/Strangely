package com.strangerchat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Fixed-window rate limiter backed by a Redis counter with TTL. Good enough
 * for Phase 1 abuse prevention (chat flooding, signaling spam) without
 * pulling in a dedicated rate-limiting library.
 */
@Service
public class RateLimitService {

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** Returns true if the action is allowed, false if the caller is over the limit. */
    public boolean allow(String bucketKey, int maxPerWindow, Duration window) {
        String key = "ratelimit:" + bucketKey;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redis.expire(key, window);
        }
        return count == null || count <= maxPerWindow;
    }
}
