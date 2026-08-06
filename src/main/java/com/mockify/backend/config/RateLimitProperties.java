package com.mockify.backend.config;

import com.mockify.backend.common.enums.RateLimitType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Configuration properties for request rate limiting.
 *
 * <p>Binds all properties under {@code mockify.rate-limit}, including:
 * <ul>
 *     <li>A global rate-limit rule applied to every request</li>
 *     <li>Group rules shared by multiple endpoints</li>
 *     <li>Endpoint-specific rules that override group rules</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "mockify.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    private Limit global;

    private Map<String, GroupLimit> groups;

    private Map<String, EndpointLimit> endpoints;

    //  Base configuration shared by all rate-limit rules.
    @Getter
    @Setter
    public static class Limit {
        private int limit;
        private Duration window;
        private RateLimitType type;
    }

    // Configuration for a group of endpoints that share the same rate-limit rule.
    @Getter
    @Setter
    public static class GroupLimit extends Limit {
        private List<String> paths;
    }

    // Configuration for a single endpoint.
    @Getter
    @Setter
    public static class EndpointLimit extends Limit {
        private String path;
        private HttpMethod method;
    }
}
