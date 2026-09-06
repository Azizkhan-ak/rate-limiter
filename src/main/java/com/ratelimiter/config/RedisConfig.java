package com.ratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

/**
 * Redis configuration.
 *
 * Spring Boot auto-configures the Lettuce connection factory from application.yml:
 *   spring.data.redis.host and spring.data.redis.port
 *
 * We just expose the beans we need on top of that.
 */
@Configuration
public class RedisConfig {

    /**
     * StringRedisTemplate — works with String keys and values.
     * Backed by Lettuce under the hood (Spring Boot default).
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    /**
     * The Lua script for sliding window rate limiting.
     * Runs atomically on Redis — no race conditions.
     *
     * TODO: You will write the Lua logic here.
     * Script receives:
     *   KEYS[1] = Redis key (e.g. "ratelimit:userId:123")
     *   ARGV[1] = current timestamp in milliseconds
     *   ARGV[2] = window size in milliseconds
     *   ARGV[3] = max requests allowed
     *
     * Script should:
     *   1. Remove entries older than (now - windowMs)
     *   2. Count remaining entries
     *   3. If count < limit → add current timestamp, return 1 (allowed)
     *   4. If count >= limit → return 0 (denied)
     */
    @Bean
    public DefaultRedisScript<List> slidingWindowScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        // TODO: set the Lua script string here
        script.setScriptText("""
        local key = KEYS[1]
        local now = tonumber(ARGV[1])
        local windowMs = tonumber(ARGV[2])
        local limit = tonumber(ARGV[3])

        redis.call('ZREMRANGEBYSCORE', key, 0, now - windowMs)
        local count = redis.call('ZCARD', key)

        if count < limit then
            redis.call('ZADD', key, now, tostring(now) .. '-' .. math.random(1000000))
            redis.call('EXPIRE', key, math.ceil(windowMs / 1000))
            local remaining = limit - count - 1
            local resetAt = math.ceil(now / 1000) + math.ceil(windowMs / 1000)
            return {1, remaining, resetAt}
        else
            local resetAt = math.ceil(now / 1000) + math.ceil(windowMs / 1000)
            return {0, 0, resetAt}
        end
    """);
        script.setResultType(List.class);
        return script;
    }
}
