package com.mockify.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ApiKeyRateLimitFilter")
class ApiKeyRateLimitFilterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ApiKeyRateLimitFilter filter;

    private MockHttpServletRequest  request;
    private MockHttpServletResponse response;
    private MockFilterChain         chain;

    private static final UUID API_KEY_ID = UUID.randomUUID();
    private static final int  LIMIT      = 5;

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain    = new MockFilterChain();
        SecurityContextHolder.clearContext();

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ApiKeyAuthenticationToken tokenWithLimit(int limit) {
        return new ApiKeyAuthenticationToken(
                API_KEY_ID,
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                List.of(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY")),
                limit
        );
    }

    private void setAuth(ApiKeyAuthenticationToken token) {
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(token);
        SecurityContextHolder.setContext(ctx);
    }

    // -------------------------------------------------------------------------
    // Pass-through scenarios
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Non-API-key requests")
    class PassThrough {

        @Test
        @DisplayName("skips filter when no authentication is present")
        void noAuthentication() throws Exception {
            // no auth set in SecurityContext
            filter.doFilterInternal(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();   // chain was called
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("skips filter for JWT-authenticated users")
        void jwtAuthentication() throws Exception {
            // Put a non-ApiKey authentication token in the context
            SecurityContext ctx = SecurityContextHolder.createEmptyContext();
            ctx.setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            "user@example.com", null,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    )
            );
            SecurityContextHolder.setContext(ctx);

            filter.doFilterInternal(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();
            verifyNoInteractions(redisTemplate);
        }
    }

    // -------------------------------------------------------------------------
    // Under limit
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Under limit")
    class UnderLimit {

        @Test
        @DisplayName("allows request when combined window count is below limit")
        void belowLimit() throws Exception {
            setAuth(tokenWithLimit(LIMIT));

            // current bucket = 2, previous bucket = 1  → total = 3 < 5
            when(valueOps.increment(anyString())).thenReturn(2L);
            when(valueOps.get(anyString())).thenReturn(1L);

            filter.doFilterInternal(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("2");
        }

        @Test
        @DisplayName("allows request when exactly at limit (boundary)")
        void exactlyAtLimit() throws Exception {
            setAuth(tokenWithLimit(LIMIT));

            // current = 3, previous = 2  → total = 5 = limit (still allowed)
            when(valueOps.increment(anyString())).thenReturn(3L);
            when(valueOps.get(anyString())).thenReturn(2L);

            filter.doFilterInternal(request, response, chain);

            assertThat(chain.getRequest()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        }

        @Test
        @DisplayName("sets all required rate-limit response headers")
        void setsHeaders() throws Exception {
            setAuth(tokenWithLimit(10));

            when(valueOps.increment(anyString())).thenReturn(1L);
            when(valueOps.get(anyString())).thenReturn(0L);

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
            assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
            assertThat(response.getHeader("Retry-After")).isNull(); // only set on 429
        }
    }

    // -------------------------------------------------------------------------
    // Over limit
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Over limit (429)")
    class OverLimit {

        @BeforeEach
        void stubObjectMapper() throws Exception {
            when(objectMapper.writeValueAsString(any())).thenReturn("{\"status\":429}");
        }

        @Test
        @DisplayName("returns 429 when combined window count exceeds limit")
        void exceeds() throws Exception {
            setAuth(tokenWithLimit(LIMIT));

            // current = 4, previous = 3  → total = 7 > 5
            when(valueOps.increment(anyString())).thenReturn(4L);
            when(valueOps.get(anyString())).thenReturn(3L);

            filter.doFilterInternal(request, response, chain);

            assertThat(chain.getRequest()).isNull();  // chain must NOT be called
            assertThat(response.getStatus()).isEqualTo(429);
            assertThat(response.getHeader("Retry-After")).isNotNull();
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        }

        @Test
        @DisplayName("does not forward request to controller on 429")
        void chainNotCalled() throws Exception {
            setAuth(tokenWithLimit(1));

            when(valueOps.increment(anyString())).thenReturn(1L);
            when(valueOps.get(anyString())).thenReturn(2L); // prev = 2, total = 3 > 1

            filter.doFilterInternal(request, response, chain);

            assertThat(chain.getRequest()).isNull();
        }

        @Test
        @DisplayName("sets Content-Type to application/json on 429")
        void contentType() throws Exception {
            setAuth(tokenWithLimit(1));

            when(valueOps.increment(anyString())).thenReturn(2L);
            when(valueOps.get(anyString())).thenReturn(0L);

            filter.doFilterInternal(request, response, chain);

            assertThat(response.getContentType()).contains("application/json");
        }
    }

    // -------------------------------------------------------------------------
    // Redis failure — fail open
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Redis failure — fail open")
    class RedisFailure {

        @Test
        @DisplayName("allows request when Redis throws on increment")
        void redisIncrementThrows() throws Exception {
            setAuth(tokenWithLimit(LIMIT));

            when(valueOps.increment(anyString())).thenThrow(new RuntimeException("Redis down"));
            // get() is not reached when increment throws, but stub a safe value anyway
            when(valueOps.get(anyString())).thenReturn(0L);

            filter.doFilterInternal(request, response, chain);

            // Should fail open — request passes through
            assertThat(chain.getRequest()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }

        @Test
        @DisplayName("allows request when Redis throws on get (previous window)")
        void redisGetThrows() throws Exception {
            setAuth(tokenWithLimit(LIMIT));

            when(valueOps.increment(anyString())).thenReturn(1L);
            when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis timeout"));

            filter.doFilterInternal(request, response, chain);

            // Previous-window count defaults to 0 on error; total = 1 < 5
            assertThat(chain.getRequest()).isNotNull();
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    // -------------------------------------------------------------------------
    // Redis key structure
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Redis key structure")
    class KeyStructure {

        @Test
        @DisplayName("uses separate Redis keys for different API keys")
        void separateKeysPerApiKey() throws Exception {
            UUID key1 = UUID.randomUUID();
            UUID key2 = UUID.randomUUID();

            ApiKeyAuthenticationToken token1 = new ApiKeyAuthenticationToken(
                    key1, UUID.randomUUID(), UUID.randomUUID(), null, List.of(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY")), 10);
            ApiKeyAuthenticationToken token2 = new ApiKeyAuthenticationToken(
                    key2, UUID.randomUUID(), UUID.randomUUID(), null, List.of(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY")), 10);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            when(valueOps.increment(anyString())).thenReturn(1L);
            when(valueOps.get(anyString())).thenReturn(0L);

            // Request 1
            setAuth(token1);
            filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

            // Request 2
            setAuth(token2);
            filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

            verify(valueOps, times(2)).increment(keyCaptor.capture());
            List<String> capturedKeys = keyCaptor.getAllValues();

            assertThat(capturedKeys.get(0)).contains(key1.toString());
            assertThat(capturedKeys.get(1)).contains(key2.toString());
            assertThat(capturedKeys.get(0)).isNotEqualTo(capturedKeys.get(1));
        }

        @Test
        @DisplayName("key format is rate_limit:{apiKeyId}:{windowIndex}")
        void keyFormat() throws Exception {
            setAuth(tokenWithLimit(LIMIT));

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            when(valueOps.increment(keyCaptor.capture())).thenReturn(1L);
            when(valueOps.get(anyString())).thenReturn(0L);

            filter.doFilterInternal(request, response, chain);

            String capturedKey = keyCaptor.getValue();
            assertThat(capturedKey).startsWith("rate_limit:");
            assertThat(capturedKey).contains(API_KEY_ID.toString());
            // format: rate_limit:{uuid}:{windowIndex}
            String[] parts = capturedKey.split(":");
            assertThat(parts).hasSizeGreaterThanOrEqualTo(3);
        }

        @Test
        @DisplayName("sets TTL on Redis key after increment")
        void setsTtl() throws Exception {
            setAuth(tokenWithLimit(LIMIT));

            when(valueOps.increment(anyString())).thenReturn(1L);
            when(valueOps.get(anyString())).thenReturn(0L);

            filter.doFilterInternal(request, response, chain);

            verify(redisTemplate).expire(anyString(), eq(Duration.ofSeconds(120)));
        }
    }
}