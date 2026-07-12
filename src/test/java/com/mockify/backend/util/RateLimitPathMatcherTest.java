package com.mockify.backend.util;

import com.mockify.backend.config.RateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RateLimitPathMatcherTest {

    private RateLimitPathMatcher pathMatcher;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties();

        // Setup Endpoint Limit
        RateLimitProperties.EndpointLimit loginEndpoint = new RateLimitProperties.EndpointLimit();
        loginEndpoint.setPath("/api/auth/login");
        loginEndpoint.setMethod(HttpMethod.POST);
        properties.setEndpoints(Map.of("login", loginEndpoint));

        // Setup Group Limit
        RateLimitProperties.GroupLimit usersGroup = new RateLimitProperties.GroupLimit();
        usersGroup.setPaths(List.of("/api/users/**"));
        properties.setGroups(Map.of("users", usersGroup));

        pathMatcher = new RateLimitPathMatcher(properties);
    }

    @Test
    void shouldMatchSpecificEndpointWithCorrectMethod() {
        var match = pathMatcher.match("/api/auth/login", HttpMethod.POST);

        assertNotNull(match);
        assertEquals("endpoint:login", match.ruleName());
        assertInstanceOf(RateLimitProperties.EndpointLimit.class, match.limit());
    }

    @Test
    void shouldNotMatchEndpointIfMethodIsDifferent() {
        // Path matches but method is GET, not POST
        var match = pathMatcher.match("/api/auth/login", HttpMethod.GET);

        assertNull(match); // Should fall through
    }

    @Test
    void shouldMatchGroupPattern() {
        var match = pathMatcher.match("/api/users/123/profile", HttpMethod.GET);

        assertNotNull(match);
        assertEquals("group:users", match.ruleName());
        assertInstanceOf(RateLimitProperties.GroupLimit.class, match.limit());
    }

    @Test
    void shouldReturnNullWhenNoMatchFound() {
        var match = pathMatcher.match("/api/unknown/path", HttpMethod.GET);

        assertNull(match);
    }
}