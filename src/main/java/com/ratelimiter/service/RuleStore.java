package com.ratelimiter.service;

import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.model.RateLimitRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Looks up the rate limit rule that applies to a given clientId and route.
 *
 * Priority order:
 *   1. clientId + route match (most specific)
 *   2. clientId match only
 *   3. route match only
 *   4. default rule (clientId = "*")
 *
 * TODO: Implement resolve() — iterate through rules and apply priority logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleStore {

    private final RateLimiterProperties properties;

    /**
     * Returns the most specific rule for this clientId + route.
     * Returns empty if no rule matches (request should be allowed by default).
     */
    public Optional<RateLimitRule> resolve(String clientId, String route) {
        // TODO: implement rule resolution with priority

           // first priority is to get rule for this clientId + Route

        Optional<RateLimitRule> rateLimitRule;
        rateLimitRule = properties.getRules().stream().filter(rule ->
                   rule.getClientId().equalsIgnoreCase(clientId) &&
                           (rule.getRoute() !=null && rule.getRoute().equalsIgnoreCase(route))).findFirst();
        if(rateLimitRule.isPresent()){
            return rateLimitRule;
        }

        rateLimitRule = properties.getRules().stream().filter(rule ->
                rule.getClientId().equalsIgnoreCase(clientId)  &&
                rule.getRoute() == null ).findFirst();
        if(rateLimitRule.isPresent()){
            return rateLimitRule;
        }

        rateLimitRule = properties.getRules().stream().filter(rule ->
                (rule.getClientId() == null || rule.getClientId().equalsIgnoreCase("*") )&&
                         rule.getRoute() != null &&
                         rule.getRoute().equalsIgnoreCase(route)).findFirst();
        if(rateLimitRule.isPresent()){
            return rateLimitRule;
        }

        return properties.getRules().stream().filter(rule ->
                rule.getClientId().equalsIgnoreCase("*")).findFirst(); //efault
        }
}
