package com.ratelimiter.config;

import com.ratelimiter.model.RateLimitRule;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds rate limiter rules from application.yml
 *
 * Example yml:
 *
 * rate-limiter:
 *   rules:
 *     - clientId: "*"
 *       limit: 10
 *       windowSeconds: 60
 *       targetUrl: "https://jsonplaceholder.typicode.com"
 */
@Data
@Component
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    private List<RateLimitRule> rules = new ArrayList<>();
}
