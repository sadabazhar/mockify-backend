package com.mockify.backend.service;

import com.mockify.backend.common.enums.RateLimitType;
import com.mockify.backend.config.RateLimitProperties;
import com.mockify.backend.dto.response.ratelimit.RateLimitResult;
import com.mockify.backend.infrastructure.RedisRateLimiter;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.util.RateLimitPathMatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final RateLimitProperties properties;
    private final RateLimitPathMatcher pathMatcher;
    private final RedisRateLimiter redisRateLimiter;

    /**
     * Checks whether the current request is allowed based on the configured
     * global and endpoint/group-specific rate limit rules.
     *
     * Flow:
     * 1. Apply the global rate limit.
     * 2. If allowed, look for a matching endpoint or group rule.
     * 3. Apply the matched rule if one exists.
     */
    public RateLimitResult checkRateLimit(String path, HttpMethod method, String ip) {

        // Always enforce the global rate limit before evaluating specific rules.
        var global = properties.getGlobal();

        String globalIdentifier = resolveIdentifier(global.getType(), ip);
        String globalKey = buildKey(global.getType(), globalIdentifier, "global");

        RateLimitResult globalResult = redisRateLimiter.check(
                globalKey,
                global.getLimit(),
                global.getWindow()
        );

        // Reject immediately if the global limit has already been exceeded.
        if (!globalResult.allowed()) {
            return globalResult;
        }

        // Find the most specific matching rule (endpoint or group).
        var match = pathMatcher.match(path, method);

        // If no rule matches, the global rate limit is the only restriction.
        if (match == null) {
            return globalResult;
        }

        // Extract values from the new record structure
        var ruleLimit = match.limit();
        String ruleName = match.ruleName();

        String identifier = resolveIdentifier(ruleLimit.getType(), ip);
        String key = buildKey(ruleLimit.getType(), identifier, ruleName);

        return redisRateLimiter.check(
                key,
                ruleLimit.getLimit(),
                ruleLimit.getWindow()
        );
    }

    /**
     * Resolves the identifier used for rate limiting.
     *
     * <ul>
     *     <li>IP limits → client IP address</li>
     *     <li>USER limits → authenticated user's UUID</li>
     *     <li>Unauthenticated requests → "anonymous"</li>
     * </ul>
     */
    private String resolveIdentifier(RateLimitType type, String ip) {

        if (RateLimitType.IP == type) {
            return ip;
        }

        // Use the authenticated user's UUID when available.
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {

            UUID userId = SecurityUtils.resolveUserId(auth);

            return userId.toString();
        }

        // Anonymous users share a common identifier for USER-based limits.
        return "anonymous";
    }

    /**
     * Builds a Redis key for storing rate limit counters.
     *
     * Format:
     * rate:{type}:{identifier}:{ruleName}
     */
    private String buildKey(RateLimitType type, String identifier, String ruleName) {
        return "rate:" + type + ":" + identifier + ":" + ruleName;
    }
}