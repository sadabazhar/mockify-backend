package com.mockify.backend.security;

import com.mockify.backend.config.RateLimitProperties;
import com.mockify.backend.dto.response.ratelimit.RateLimitResult;
import com.mockify.backend.service.RateLimitService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private RateLimitProperties properties;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {


        // Endpoint
        RateLimitProperties.EndpointLimit loginEndpoint = new RateLimitProperties.EndpointLimit();
        loginEndpoint.setPath("/api/auth/login");
        loginEndpoint.setMethod(HttpMethod.POST);

        // Group
        RateLimitProperties.GroupLimit authGroup = new RateLimitProperties.GroupLimit();
        authGroup.setPaths(List.of("/api/auth/**"));

        properties.setEndpoints(Map.of("login", loginEndpoint));
        properties.setGroups(Map.of("auth", authGroup));

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void shouldSkipExcludedPaths() throws Exception {
        request.setRequestURI("/swagger-ui/index.html");

        rateLimitFilter.doFilter(request, response, filterChain);

        verify(rateLimitService, never())
                .checkRateLimit(anyString(), any(), anyString());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldExtractRealIpFromForwardedHeader() throws Exception {
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        // Simulated Cloudflare/Nginx forwarded header
        request.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1");

        RateLimitResult allowedResult = new RateLimitResult(true, 100, 99, Instant.now().getEpochSecond());
        when(rateLimitService.checkRateLimit(anyString(), any(HttpMethod.class), eq("203.0.113.195")))
                .thenReturn(allowedResult);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(rateLimitService).checkRateLimit("/api/test", HttpMethod.GET, "203.0.113.195");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAddRateLimitHeadersAndProceedWhenAllowed() throws Exception {
        request.setRequestURI("/api/test");
        request.setMethod("POST");
        request.setRemoteAddr("127.0.0.1");

        long resetTime = Instant.now().getEpochSecond() + 60;
        RateLimitResult allowedResult = new RateLimitResult(true, 100, 50, resetTime);
        when(rateLimitService.checkRateLimit("/api/test", HttpMethod.POST, "127.0.0.1"))
                .thenReturn(allowedResult);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals("100", response.getHeader("X-RateLimit-Limit"));
        assertEquals("50", response.getHeader("X-RateLimit-Remaining"));
        assertEquals(String.valueOf(resetTime), response.getHeader("X-RateLimit-Reset"));
        assertNull(response.getHeader("Retry-After")); // Not present on success

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldBlockRequestAndReturn429WhenLimitExceeded() throws Exception {
        request.setRequestURI("/api/test");
        request.setMethod("GET");
        request.setRemoteAddr("127.0.0.1");

        // Set reset time 10 seconds in the future
        long resetTime = Instant.now().getEpochSecond() + 10;
        RateLimitResult blockedResult = new RateLimitResult(false, 100, 0, resetTime);
        when(rateLimitService.checkRateLimit("/api/test", HttpMethod.GET, "127.0.0.1"))
                .thenReturn(blockedResult);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        assertEquals(429, response.getStatus());
        assertEquals("application/json", response.getContentType());

        // Verify Retry-After header is correctly calculated (approx 10)
        String retryAfter = response.getHeader("Retry-After");
        assertNotNull(retryAfter);
        assertTrue(Integer.parseInt(retryAfter) <= 10 && Integer.parseInt(retryAfter) >= 8);

        // Verify JSON response body
        String responseBody = response.getContentAsString();
        assertTrue(responseBody.contains("Rate limit exceeded"));

        // Verify filter chain was stopped
        verify(filterChain, never()).doFilter(request, response);
    }

}
