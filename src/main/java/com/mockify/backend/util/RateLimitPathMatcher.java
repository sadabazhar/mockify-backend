package com.mockify.backend.util;

import com.mockify.backend.config.RateLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
@RequiredArgsConstructor
public class RateLimitPathMatcher {

    private final RateLimitProperties properties;
    private final AntPathMatcher matcher = new AntPathMatcher();

    /**
     * Finds the most appropriate rate-limit rule for the incoming request.
     *
     * Matching priority:
     * <ol>
     *     <li>Endpoint-specific rules (highest priority)</li>
     *     <li>Group rules</li>
     * </ol>
     *
     * @return the matched rule, or {@code null} if no rule applies
     */
    public RateLimitMatch match(String path, HttpMethod method) {

        // Endpoint rules take precedence over group rules.
        if (properties.getEndpoints() != null) {
            for (var entry : properties.getEndpoints().entrySet()) {
                var endpoint = entry.getValue();

                // Match only when both the HTTP method and path are configured
                // and match the incoming request.
                boolean methodMatches = endpoint.getMethod() != null && endpoint.getMethod().equals(method);
                boolean pathMatches = endpoint.getPath() != null && matcher.match(endpoint.getPath(), path);

                if (methodMatches && pathMatches) {
                    return new RateLimitMatch("endpoint:" + entry.getKey(), endpoint);
                }
            }
        }

        // If no endpoint rule matches, fall back to group-based rules.
        if (properties.getGroups() != null) {
            for (var entry : properties.getGroups().entrySet()) {
                String groupName = entry.getKey();
                var group = entry.getValue();

                for (String pattern : group.getPaths()) {
                    if (matcher.match(pattern, path)) {
                        return new RateLimitMatch("group:" + groupName, group);
                    }
                }
            }
        }

        // No configured rate-limit rule matches this request.
        return null;
    }

    /**
     * Represents the matched rate-limit rule.
     *
     * The {@code limit} field uses the shared {@link RateLimitProperties.Limit}
     * type, allowing this record to represent either an endpoint rule or a
     * group rule.
     */
    public record RateLimitMatch(
            String ruleName,
            RateLimitProperties.Limit limit
    ) {}
}
