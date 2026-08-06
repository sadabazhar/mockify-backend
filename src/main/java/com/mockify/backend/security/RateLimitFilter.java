package com.mockify.backend.security;

import com.mockify.backend.dto.response.ratelimit.RateLimitResult;
import com.mockify.backend.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * Request paths excluded from rate limiting.
     *
     * <p>These endpoints serve documentation, OAuth callbacks, or framework
     * resources and should remain accessible without consuming rate-limit quotas.
     */
    private static final List<String> EXCLUDED_PATHS = List.of(
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/.well-known/**"
    );

    /**
     * Determines whether rate limiting should be skipped for the current request.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();

        return EXCLUDED_PATHS
                .stream()
                .anyMatch(pattern -> matcher.match(pattern, path));
    }


    /**
     * Applies request rate limiting before passing the request further down the
     * Spring Security filter chain.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Convert raw string to HttpMethod safely. Null safe fallback to GET.
        String rawMethod = request.getMethod();
        HttpMethod method = rawMethod != null ? HttpMethod.valueOf(rawMethod) : HttpMethod.GET;

        String ip = getClientIp(request);

        // Pass method to service
        RateLimitResult result = rateLimitService.checkRateLimit(path, method, ip);

        // Expose the current rate-limit status using standard response headers.
        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetEpochSec()));

        if (!result.allowed()) {

            // Log the violation
            log.warn("Rate limit exceeded for IP: {} on Path: {} {}", ip, method, path);

            // Tell the client how long to wait before making another request.
            long retryAfter = result.resetEpochSec() - Instant.now().getEpochSecond();
            response.setHeader("Retry-After", String.valueOf(Math.max(retryAfter,0)));
            response.setStatus(429);
            response.setContentType("application/json");

            response.getWriter().write("""
                    {
                      "error": "Too many requests",
                      "message": "Rate limit exceeded"
                    }
                    """);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the client's IP address.
     *
     * <p>Prefers the first IP from the {@code X-Forwarded-For} header when the
     * application is running behind a reverse proxy, otherwise falls back to the
     * remote socket address.
     */
    private String getClientIp(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0];
        }

        return request.getRemoteAddr();
    }
}