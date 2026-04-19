package com.mockify.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.dto.response.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Sliding-window rate limiter for API key authenticated requests.
 *
 * <p>Runs immediately after {@link ApiKeyAuthenticationFilter} (Order 2).
 * Only applies to requests that carry a valid {@link ApiKeyAuthenticationToken};
 * JWT-authenticated and unauthenticated requests pass through untouched.</p>
 *
 * <h3>Algorithm — Redis atomic sliding window</h3>
 * <pre>
 *   key  = "rate_limit:{apiKeyId}:{windowStart}"   (windowStart = epoch-second / 60)
 *   INCR key          → current count in this 1-minute bucket
 *   EXPIRE key 120s   → keep previous window alive for soft-overlap reads
 * </pre>
 * <p>Two buckets (current + previous minute) are summed to give a smooth
 * sliding-window count with no hard boundary spikes.</p>
 *
 * <h3>Response headers</h3>
 * <ul>
 *   <li>{@code X-RateLimit-Limit}     — configured limit for this key</li>
 *   <li>{@code X-RateLimit-Remaining} — requests left in the current window</li>
 *   <li>{@code X-RateLimit-Reset}     — Unix epoch second when the window resets</li>
 *   <li>{@code Retry-After}           — seconds to wait (only on 429)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyRateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    /** Redis TTL: two full minutes keeps the previous-window bucket readable. */
    private static final long BUCKET_TTL_SECONDS = 120;
    private static final int  WINDOW_SECONDS      = 60;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    // -------------------------------------------------------------------------
    // Filter entry point
    // -------------------------------------------------------------------------

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Only rate-limit API key authenticated requests
        if (!(auth instanceof ApiKeyAuthenticationToken token)) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID    apiKeyId = token.getApiKeyId();
        int     limit    = token.getRateLimitPerMinute();
        long    now      = System.currentTimeMillis() / 1_000L;   // epoch seconds
        long    window   = now / WINDOW_SECONDS;                   // current 1-min bucket index

        long currentCount  = increment(apiKeyId, window);
        long previousCount = getCount(apiKeyId, window - 1);
        long totalCount    = currentCount + previousCount;

        long windowResetAt = (window + 1) * WINDOW_SECONDS;       // next bucket boundary
        long remaining     = Math.max(0L, limit - totalCount);

        // Always set informational headers
        response.setHeader("X-RateLimit-Limit",     String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset",     String.valueOf(windowResetAt));

        if (totalCount > limit) {
            long retryAfter = windowResetAt - now;
            response.setHeader("Retry-After", String.valueOf(retryAfter));

            log.warn("Rate limit exceeded: apiKeyId={}, count={}/{}, ip={}",
                    apiKeyId, totalCount, limit, request.getRemoteAddr());

            sendRateLimitExceeded(request, response, retryAfter);
            return;
        }

        log.debug("Rate limit check: apiKeyId={}, count={}/{}", apiKeyId, totalCount, limit);
        filterChain.doFilter(request, response);
    }

    // -------------------------------------------------------------------------
    // Redis helpers
    // -------------------------------------------------------------------------

    /**
     * Atomically increment the counter for {@code apiKeyId} in {@code window}
     * and set (or refresh) a 120-second TTL so stale buckets self-expire.
     *
     * @return the new counter value after increment
     */
    private long increment(UUID apiKeyId, long window) {
        String key = buildKey(apiKeyId, window);
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            // Set TTL on first write; on subsequent writes it is a no-op because
            // the key already has a TTL.  Using EXPIRE is cheaper than a Lua script
            // for this straightforward case.
            redisTemplate.expire(key, Duration.ofSeconds(BUCKET_TTL_SECONDS));
            return count != null ? count : 1L;
        } catch (Exception e) {
            log.error("Redis error incrementing rate-limit counter: key={}", key, e);
            // Fail open: let the request through rather than blocking all traffic
            // on a Redis outage.
            return 0L;
        }
    }

    /**
     * Read the counter for the previous window without modifying it.
     *
     * @return counter value, or 0 if the key is absent or Redis is unavailable
     */
    private long getCount(UUID apiKeyId, long window) {
        String key = buildKey(apiKeyId, window);
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw == null) return 0L;
            // RedisTemplate stores Long as Integer when deserialized with
            // GenericJackson2JsonRedisSerializer on some configurations.
            if (raw instanceof Number n) return n.longValue();
            return Long.parseLong(raw.toString());
        } catch (Exception e) {
            log.error("Redis error reading rate-limit counter: key={}", key, e);
            return 0L;
        }
    }

    private String buildKey(UUID apiKeyId, long window) {
        return RATE_LIMIT_KEY_PREFIX + apiKeyId + ":" + window;
    }

    // -------------------------------------------------------------------------
    // Error response
    // -------------------------------------------------------------------------

    private void sendRateLimitExceeded(
            HttpServletRequest request,
            HttpServletResponse response,
            long retryAfterSeconds) throws IOException {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("Too Many Requests")
                .message("API key rate limit exceeded. Retry after " + retryAfterSeconds + " second(s).")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}