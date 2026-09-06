package com.ratelimiter.filter;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.service.RateLimiterService;
import com.ratelimiter.service.RuleStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Intercepts every incoming request.
 *
 * Responsibilities:
 *   1. Extract clientId from the request (header, query param, or IP fallback)
 *   2. Look up the applicable rule from RuleStore
 *   3. Ask RateLimiterService to evaluate the request
 *   4. If DENIED  → return 429 immediately with rate limit headers
 *   5. If ALLOWED → pass request to ProxyHandler (via filter chain)
 *
 * TODO: implement doFilterInternal()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimiterFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final RuleStore ruleStore;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // TODO:
        // 1. Extract clientId — check header "X-Client-Id", fallback to request.getRemoteAddr()
        // 2. Extract route — request.getRequestURI()
        // 3. ruleStore.resolve(clientId, route)
        // 4. If no rule found → allow (filterChain.doFilter)
        // 5. rateLimiterService.evaluate(clientId, rule)
        // 6. Add headers: X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
        // 7. If denied → response.setStatus(429), write JSON body, return
        // 8. If allowed → filterChain.doFilter(request, response)

        filterChain.doFilter(request, response); // placeholder — passes everything through
    }

    private String extractClientId(HttpServletRequest request) {
        // TODO: check X-Client-Id header first, then X-API-Key, then IP
        return request.getRemoteAddr();
    }
}
