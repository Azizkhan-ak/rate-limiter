package com.ratelimiter.service;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Core rate limiting logic.
 *
 * Delegates to the correct algorithm based on the rule.
 * Each algorithm talks to Redis to check + record the request.
 *
 * TODO: Implement each algorithm method.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> slidingWindowScript;

    /**
     * Evaluates whether this request should be allowed.
     *
     * @param clientId  identifier for the caller (userId, apiKey, IP)
     * @param rule      the rule that applies to this client+route
     * @return          RateLimitResult with allowed/denied + remaining count
     */
    public RateLimitResult evaluate(String clientId, RateLimitRule rule) {
        return switch (rule.getAlgorithm()) {
            case SLIDING_WINDOW -> slidingWindow(clientId, rule);
            case FIXED_WINDOW   -> fixedWindow(clientId, rule);
            case TOKEN_BUCKET   -> tokenBucket(clientId, rule);
        };
    }

    // -------------------------------------------------------------------------
    // SLIDING WINDOW
    // Redis key: ratelimit:sliding:{clientId}
    // Uses ZSET — score = timestamp, value = timestamp
    // Lua script handles atomic check + add + remove old entries
    // -------------------------------------------------------------------------
    private RateLimitResult slidingWindow(String clientId, RateLimitRule rule) {
        // TODO: implement sliding window using Lua script
        // 1. Build the Redis key
        // 2. Execute slidingWindowScript with current timestamp, windowMs, limit
        // 3. Parse result — 1 = allowed, 0 = denied
        // 4. Return RateLimitResult with remaining count and resetAt
        throw new UnsupportedOperationException("TODO: implement sliding window");
    }

    // -------------------------------------------------------------------------
    // FIXED WINDOW
    // Redis key: ratelimit:fixed:{clientId}:{windowBucket}
    // windowBucket = currentTimeSeconds / windowSeconds  (integer division)
    // Uses INCR + EXPIRE
    // -------------------------------------------------------------------------
    private RateLimitResult fixedWindow(String clientId, RateLimitRule rule) {
        // TODO: implement fixed window
        throw new UnsupportedOperationException("TODO: implement fixed window");
    }

    // -------------------------------------------------------------------------
    // TOKEN BUCKET
    // Redis key: ratelimit:token:{clientId}
    // Stores: tokens remaining + last refill timestamp
    // -------------------------------------------------------------------------
    private RateLimitResult tokenBucket(String clientId, RateLimitRule rule) {
        // TODO: implement token bucket
        throw new UnsupportedOperationException("TODO: implement token bucket");
    }
}
