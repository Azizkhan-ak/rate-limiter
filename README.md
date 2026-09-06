# Rate Limiter Gateway

A standalone rate limiting gateway built with Spring Boot and Redis. Intercepts incoming HTTP requests, applies configurable rate limit rules, and proxies allowed requests to upstream APIs.

## Architecture

```
Client → RateLimiterFilter → RuleStore → RateLimiterService → Redis
                                                ↓
                                         Allow → ProxyHandler → Upstream API
                                         Deny  → 429 Too Many Requests
```

### Core Components

| Class | Responsibility |
|---|---|
| `RateLimiterFilter` | Intercepts every request, extracts clientId, orchestrates the flow |
| `RuleStore` | Resolves which rate limit rule applies based on clientId + route priority |
| `RateLimiterService` | Runs the rate limiting algorithm against Redis |
| `ProxyHandler` | Forwards allowed requests to the upstream API via WebClient |

### Supporting Classes

| Class | Responsibility |
|---|---|
| `RateLimitRule` | Rule config (clientId, route, limit, windowSeconds, algorithm, targetUrl) |
| `RateLimitResult` | Result of evaluation (allowed, remaining, resetAt, limit) |
| `RateLimiterProperties` | Binds rules from `application.yml` |
| `RedisConfig` | Redis beans + Lua script for sliding window |

## Algorithms

### Fixed Window
Divides time into fixed buckets (`currentTimeSeconds / windowSeconds`). Uses Redis `INCR` + `EXPIRE`. Simple and fast but susceptible to boundary bursts.

### Sliding Window
Stores each request timestamp in a Redis ZSET. On every request, removes entries older than `now - windowMs`, counts remaining, and allows or denies. Runs atomically via a Lua script to prevent race conditions.

### Token Bucket
*(Coming soon)*

## Rule Priority

Rules are resolved in this order (most specific first):

1. `clientId` + `route` match
2. `clientId` only match
3. `route` only match
4. Default (`clientId: "*"`)

## Configuration

Rules are defined in `application.yml`:

```yaml
rate-limiter:
  rules:
    - clientId: "vip-user"
      route: "/posts/1"
      limit: 2
      windowSeconds: 60
      algorithm: FIXED_WINDOW
      targetUrl: "https://jsonplaceholder.typicode.com"

    - clientId: "*"
      limit: 10
      windowSeconds: 60
      algorithm: SLIDING_WINDOW
      targetUrl: "https://jsonplaceholder.typicode.com"
```

## Response Headers

Every response includes:

| Header | Description |
|---|---|
| `X-RateLimit-Limit` | Max requests allowed in the window |
| `X-RateLimit-Remaining` | Requests remaining in current window |
| `X-RateLimit-Reset` | Unix timestamp (seconds) when the window resets |

## Running Locally

**Prerequisites:** Docker, Java 17, Maven

```bash
# Start Redis
docker run -d -p 6379:6379 redis

# Run the gateway
mvn spring-boot:run
```

**Test:**
```bash
curl -v -H "X-Client-Id: vip-user" http://localhost:8080/posts/1
```

Hit it more than the configured limit — you'll get `429 Rate limit exceeded`.

## Client Identification

The gateway identifies clients in this priority order:

1. `X-Client-Id` header
2. `X-API-Key` header
3. IP address (fallback)

## Redis Key Structure

| Algorithm | Key Pattern |
|---|---|
| Fixed Window | `ratelimit:fixed:{clientId}:{route}:{windowBucket}` |
| Sliding Window | `ratelimit:sliding:{clientId}:{route}` |
