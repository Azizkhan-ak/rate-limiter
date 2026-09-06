package com.ratelimiter.model;

import lombok.Data;

/**
 * Represents one rate limiting rule.
 *
 * A rule can match on:
 *   - clientId only  (e.g. userId:123 → 1000 req/min)
 *   - route only     (e.g. /api/checkout → 5 req/min for everyone)
 *   - both           (e.g. userId:123 on /api/checkout → 2 req/min)
 *   - neither        (clientId="*") → default rule for all clients
 *
 * Priority: clientId+route > clientId only > route only > default (*)
 */
@Data
public class RateLimitRule {

    /** clientId this rule applies to. Use "*" for default (all clients). */
    private String clientId = "*";

    /** Route/path this rule applies to. Null means all routes. */
    private String route;

    /** Max requests allowed in the time window. */
    private int limit;

    /** Window size in seconds. */
    private int windowSeconds;

    /** Algorithm to use: SLIDING_WINDOW, FIXED_WINDOW, TOKEN_BUCKET */
    private Algorithm algorithm = Algorithm.SLIDING_WINDOW;

    /** Target backend URL to proxy to when request is allowed. */
    private String targetUrl;

    public enum Algorithm {
        FIXED_WINDOW,
        SLIDING_WINDOW,
        TOKEN_BUCKET
    }
}
