package com.ratelimiter.proxy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

/**
 * Forwards an allowed request to the target backend API and writes the response back.
 *
 * Uses Spring WebClient (non-blocking HTTP client, backed by Reactor Netty).
 *
 * Flow:
 *   RateLimiterFilter (allowed) → ProxyHandler → Target API → response back to client
 *
 * TODO: implement forward()
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProxyHandler {

    private final WebClient.Builder webClientBuilder;

    /**
     * Forwards the request to targetUrl and pipes the response back.
     *
     * @param request    original incoming HttpServletRequest
     * @param response   HttpServletResponse to write the proxied response into
     * @param targetUrl  base URL of the backend (e.g. "https://jsonplaceholder.typicode.com")
     */
    public void forward(HttpServletRequest request, HttpServletResponse response, String targetUrl) throws IOException {
        // TODO:

        log.info("Proxying {} {} → {}", request.getMethod(), request.getRequestURI(), targetUrl);

        String fullUrl = targetUrl + request.getRequestURI();
        String query = request.getQueryString();
        if (query != null) fullUrl += "?" + query;

        log.info("Full URL: {}", fullUrl);

        ResponseEntity<byte[]> upstream = webClientBuilder.build()
                .method(HttpMethod.valueOf(request.getMethod()))
                .uri(fullUrl)
                .exchangeToMono(res -> res.toEntity(byte[].class))
                .block();

        response.setStatus(upstream.getStatusCode().value());
        upstream.getHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("Transfer-Encoding") &&
                    !name.equalsIgnoreCase("Content-Encoding")) {
                values.forEach(value -> response.addHeader(name, value));
            }
        });
        if (upstream.getBody() != null) {
            response.getOutputStream().write(upstream.getBody());
        }

        // 1. Build the full target URL = targetUrl + request.getRequestURI() + query string
        // 2. Use WebClient to forward with same HTTP method, headers, body
        // 3. Write the backend's status code, headers, and body into response

    }
}
