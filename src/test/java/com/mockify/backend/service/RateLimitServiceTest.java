package com.mockify.backend.service;

import com.mockify.backend.common.enums.RateLimitType;
import com.mockify.backend.config.RateLimitProperties;
import com.mockify.backend.dto.response.ratelimit.RateLimitResult;
import com.mockify.backend.infrastructure.RedisRateLimiter;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.util.RateLimitPathMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private RateLimitProperties properties;

    @Mock
    private RateLimitPathMatcher pathMatcher;

    @Mock
    private RedisRateLimiter redisRateLimiter;

    @InjectMocks
    private RateLimitService rateLimitService;

    private RateLimitProperties.Limit globalLimit;

    @BeforeEach
    void setUp() {
        globalLimit = new RateLimitProperties.Limit();
        globalLimit.setLimit(200);
        globalLimit.setWindow(Duration.ofMinutes(1));
        globalLimit.setType(RateLimitType.IP);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnBlockedIfGlobalLimitExceeded() {
        when(properties.getGlobal()).thenReturn(globalLimit);

        RateLimitResult blockedResult = new RateLimitResult(false, 200, 0, 1000L);
        when(redisRateLimiter.check(anyString(), anyInt(), any())).thenReturn(blockedResult);

        RateLimitResult result = rateLimitService.checkRateLimit("/api/test", HttpMethod.GET, "127.0.0.1");

        assertFalse(result.allowed());
        verify(pathMatcher, never()).match(anyString(), any()); // Shouldn't check specific paths if global blocks
    }

    @Test
    void shouldCheckSpecificRuleIfGlobalAllowed() {
        when(properties.getGlobal()).thenReturn(globalLimit);
        RateLimitResult allowedResult = new RateLimitResult(true, 200, 199, 1000L);

        // Global is allowed
        when(redisRateLimiter.check(contains("global"), anyInt(), any())).thenReturn(allowedResult);

        // Specific Endpoint Match
        RateLimitProperties.EndpointLimit endpointLimit = new RateLimitProperties.EndpointLimit();
        endpointLimit.setType(RateLimitType.IP);
        endpointLimit.setLimit(10);

        RateLimitPathMatcher.RateLimitMatch match = new RateLimitPathMatcher.RateLimitMatch("endpoint:login", endpointLimit);
        when(pathMatcher.match("/api/auth/login", HttpMethod.POST)).thenReturn(match);

        // Specific result is blocked
        RateLimitResult specificBlockedResult = new RateLimitResult(false, 10, 0, 1000L);
        when(redisRateLimiter.check(contains("endpoint:login"), anyInt(), any())).thenReturn(specificBlockedResult);

        RateLimitResult result = rateLimitService.checkRateLimit("/api/auth/login", HttpMethod.POST, "127.0.0.1");

        assertFalse(result.allowed());
        assertEquals(10, result.limit());
    }

    @Test
    void shouldResolveUserIdWhenTypeIsUser() {
        when(properties.getGlobal()).thenReturn(globalLimit);
        when(redisRateLimiter.check(contains("global"), anyInt(), any())).thenReturn(new RateLimitResult(true, 200, 199, 1000L));

        // Setup specific rule requiring USER type
        RateLimitProperties.GroupLimit groupLimit = new RateLimitProperties.GroupLimit();
        groupLimit.setType(RateLimitType.USER);
        RateLimitPathMatcher.RateLimitMatch match = new RateLimitPathMatcher.RateLimitMatch("group:dashboard", groupLimit);
        when(pathMatcher.match(anyString(), any())).thenReturn(match);

        // Mock Security Context
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("someUser");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        UUID expectedUserId = UUID.randomUUID();

        // Mock static method SecurityUtils.resolveUserId
        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(() -> SecurityUtils.resolveUserId(auth)).thenReturn(expectedUserId);

            // Execute
            rateLimitService.checkRateLimit("/api/dashboard", HttpMethod.GET, "127.0.0.1");

            // Verify Redis was called with the correct user UUID key
            verify(redisRateLimiter).check(
                    eq("rate:USER:" + expectedUserId.toString() + ":group:dashboard"),
                    anyInt(),
                    any()
            );
        }
    }

    @Test
    void shouldReturnGlobalResultWhenNoSpecificRuleMatches() {

        when(properties.getGlobal()).thenReturn(globalLimit);

        RateLimitResult globalResult =
                new RateLimitResult(true, 200, 199, 1000L);

        when(redisRateLimiter.check(
                contains("global"),
                anyInt(),
                any()
        )).thenReturn(globalResult);

        when(pathMatcher.match(anyString(), any()))
                .thenReturn(null);

        RateLimitResult result =
                rateLimitService.checkRateLimit(
                        "/api/test",
                        HttpMethod.GET,
                        "127.0.0.1"
                );

        assertTrue(result.allowed());
        assertEquals(200, result.limit());

        verify(pathMatcher)
                .match("/api/test", HttpMethod.GET);

        verify(redisRateLimiter, times(1))
                .check(anyString(), anyInt(), any());
    }

    @Test
    void shouldUseAnonymousIdentifierWhenUserIsNotAuthenticated() {

        when(properties.getGlobal()).thenReturn(globalLimit);

        when(redisRateLimiter.check(
                contains("global"),
                anyInt(),
                any()
        )).thenReturn(
                new RateLimitResult(true, 200, 199, 1000L)
        );

        RateLimitProperties.GroupLimit groupLimit =
                new RateLimitProperties.GroupLimit();

        groupLimit.setType(RateLimitType.USER);
        groupLimit.setLimit(60);
        groupLimit.setWindow(Duration.ofMinutes(1));

        when(pathMatcher.match(anyString(), any()))
                .thenReturn(
                        new RateLimitPathMatcher.RateLimitMatch(
                                "group:dashboard",
                                groupLimit
                        )
                );

        SecurityContext context = mock(SecurityContext.class);

        when(context.getAuthentication()).thenReturn(null);

        SecurityContextHolder.setContext(context);

        rateLimitService.checkRateLimit(
                "/api/dashboard",
                HttpMethod.GET,
                "127.0.0.1"
        );

        verify(redisRateLimiter).check(
                eq("rate:USER:anonymous:group:dashboard"),
                eq(60),
                eq(Duration.ofMinutes(1))
        );
    }

    @Test
    void shouldUseUserIdentifierForGlobalLimitWhenConfigured() {

        globalLimit.setType(RateLimitType.USER);

        when(properties.getGlobal()).thenReturn(globalLimit);

        when(redisRateLimiter.check(
                contains("global"),
                anyInt(),
                any()
        )).thenReturn(
                new RateLimitResult(true, 200, 199, 1000L)
        );

        when(pathMatcher.match(anyString(), any()))
                .thenReturn(null);

        Authentication auth = mock(Authentication.class);

        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("user");

        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(auth);

        SecurityContextHolder.setContext(securityContext);

        UUID userId = UUID.randomUUID();

        try (MockedStatic<SecurityUtils> mockedStatic =
                     Mockito.mockStatic(SecurityUtils.class)) {

            mockedStatic.when(() -> SecurityUtils.resolveUserId(auth))
                    .thenReturn(userId);

            rateLimitService.checkRateLimit(
                    "/api/test",
                    HttpMethod.GET,
                    "127.0.0.1"
            );

            verify(redisRateLimiter).check(
                    eq("rate:USER:" + userId + ":global"),
                    eq(200),
                    eq(Duration.ofMinutes(1))
            );
        }
    }
}
