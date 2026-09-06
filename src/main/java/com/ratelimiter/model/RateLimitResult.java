package com.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Result returned by RateLimiterService after evaluating a request.
 */
@Data
@AllArgsConstructor
public class RateLimitResult {

    /** Whether the request is allowed to proceed. */
    private boolean allowed;

    /** How many requests the client has remaining in this window. */
    private long remaining;

    /** Unix timestamp (seconds) when the window resets. */
    private long resetAt;

    /** The limit that was applied. */
    private int limit;
}
