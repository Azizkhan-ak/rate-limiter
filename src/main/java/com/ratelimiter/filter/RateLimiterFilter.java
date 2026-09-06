package com.ratelimiter.filter;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.model.RateLimitRule;
import com.ratelimiter.proxy.ProxyHandler;
import com.ratelimiter.service.RateLimiterService;
import com.ratelimiter.service.RuleStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final ProxyHandler proxyHandler;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // TODO:

        String clientId = extractClientId(request);
        String route = request.getRequestURI();

        Optional<RateLimitRule> rateLimitRule = ruleStore.resolve(clientId,route);

        if(rateLimitRule.isPresent()){
            RateLimitResult rateLimitResult = rateLimiterService.evaluate(clientId,rateLimitRule.get());
            if (rateLimitResult.isAllowed()) {
                response.addHeader("X-RateLimit-Limit", String.valueOf(rateLimitResult.getLimit()));
                response.addHeader("X-RateLimit-Remaining", String.valueOf(rateLimitResult.getRemaining()));
                response.addHeader("X-RateLimit-Reset", String.valueOf(rateLimitResult.getResetAt()));
                proxyHandler.forward(request,response,rateLimitRule.get().getTargetUrl());
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
                response.addHeader("X-RateLimit-Limit", String.valueOf(rateLimitResult.getLimit()));
                response.addHeader("X-RateLimit-Remaining", "0");
                response.addHeader("X-RateLimit-Reset", String.valueOf(rateLimitResult.getResetAt()));
                response.getWriter().write("Rate limit exceeded");
            }
        }
    }

    private String extractClientId(HttpServletRequest request) {
        // TODO: check X-Client-Id header first, then X-API-Key, then IP
        String clientId = request.getHeader("X-Client-Id");
        if (clientId != null && !clientId.isBlank()) return clientId;

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) return apiKey;

        return request.getRemoteAddr();
    }
}
