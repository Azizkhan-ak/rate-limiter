package com.ratelimiter.service;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
    private final DefaultRedisScript<List> slidingWindowScript;

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
    // Redis key: ratelimit:sliding:{clientId}:{route}
    // Uses ZSET — score = timestamp, value = uuid
    // Lua script handles atomic check + add + remove old entries
    // -------------------------------------------------------------------------
    private RateLimitResult slidingWindow(String clientId, RateLimitRule rule) {
        // TODO: implement sliding window using Lua script

        String key = "ratelimit:sliding:{"+clientId+"}:{"+rule.getRoute()+"}";
        long now = System.currentTimeMillis();
        long windowMs = rule.getWindowSeconds() * 1000L;

        List<Long> result = redisTemplate.execute(
                slidingWindowScript,
                List.of(key),
                String.valueOf(now),
                String.valueOf(windowMs),
                String.valueOf(rule.getLimit())
        );

        boolean allowed = result.get(0) == 1L;
        long remaining  = result.get(1);
        long resetAt    = result.get(2);

        return RateLimitResult.builder()
                    .limit(rule.getLimit())
                    .allowed(allowed)
                    .resetAt(resetAt)
                    .remaining(remaining).build();

        // 1. Build the Redis key
        // 2. Execute slidingWindowScript with current timestamp, windowMs, limit
        // 3. Parse result — 1 = allowed, 0 = denied
        // 4. Return RateLimitResult with remaining count and resetAt
    }

    // -------------------------------------------------------------------------
    // FIXED WINDOW
    // Redis key: ratelimit:fixed:{clientId}:{windowBucket}
    // windowBucket = currentTimeSeconds / windowSeconds  (integer division)
    // Uses INCR + EXPIRE
    // -------------------------------------------------------------------------
    private RateLimitResult fixedWindow(String clientId, RateLimitRule rule) {

        Long currentBucket = ((System.currentTimeMillis()/1000)/rule.getWindowSeconds());
        String key = "ratelimit:fixed:{"+clientId+"}:{"+rule.getRoute()+"}:{"+currentBucket+"}";
        Long counter = redisTemplate.opsForValue().increment(key);

        if(counter == 1){
            redisTemplate.expire(key, rule.getWindowSeconds(), TimeUnit.SECONDS);
        }

        return RateLimitResult.builder()
                .allowed(rule.getLimit()>=counter)
                .remaining(Math.max(0, rule.getLimit() - counter))
                .resetAt((currentBucket + 1) * rule.getWindowSeconds())
                .limit(rule.getLimit())
                .build();
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
