package com.mockify.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.sandbox.SandboxSession;
import com.mockify.backend.service.SandboxService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SandboxRequestInterceptor")
class SandboxRequestInterceptorTest {

    @Mock private SandboxService sandboxService;
    @Mock private FilterChain filterChain;

    // Use a real ObjectMapper so error response serialisation is verified properly
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @InjectMocks
    private SandboxRequestInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private SandboxSession activeSession;

    @BeforeEach
    void setUp() {
        request  = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        activeSession = SandboxSession.builder()
                .userId(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .createdAtEpochMs(System.currentTimeMillis())
                .hardExpiresAtEpochMs(System.currentTimeMillis() + 86_400_000L)
                .build();

        // Set the ObjectMapper via reflection since @InjectMocks won't use our real instance
        org.springframework.test.util.ReflectionTestUtils.setField(
                interceptor, "objectMapper", objectMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =========================================================================
    // Pass-through: non-guest callers
    // =========================================================================

    @Nested
    @DisplayName("Pass-through conditions")
    class PassThrough {

        @Test
        @DisplayName("No authentication — passes through without Redis lookup")
        void noAuth_passesThrough() throws Exception {
            SecurityContextHolder.clearContext();
            request.setServletPath("/api/organizations");

            interceptor.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(sandboxService);
        }

        @Test
        @DisplayName("ROLE_USER authentication — passes through without Redis lookup")
        void userAuth_passesThrough() throws Exception {
            setAuthentication("ROLE_USER");
            request.setServletPath("/api/organizations");

            interceptor.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(sandboxService);
        }

        @Test
        @DisplayName("ROLE_ADMIN authentication — passes through without Redis lookup")
        void adminAuth_passesThrough() throws Exception {
            setAuthentication("ROLE_ADMIN");
            request.setServletPath("/api/admin/users");

            interceptor.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(sandboxService);
        }

        @Test
        @DisplayName("API key authentication — passes through without Redis lookup")
        void apiKeyAuth_passesThrough() throws Exception {
            setAuthentication("ROLE_API_KEY");
            request.setServletPath("/api/my-org/my-project/schema/records");

            interceptor.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(sandboxService);
        }
    }

    // =========================================================================
    // shouldNotFilter — sandbox and public paths skipped
    // =========================================================================

    @Nested
    @DisplayName("shouldNotFilter()")
    class ShouldNotFilter {

        @Test
        @DisplayName("Skips /api/sandbox/** paths")
        void skips_sandboxPaths() {
            request.setServletPath("/api/sandbox/start");
            assertThat(interceptor.shouldNotFilter(request)).isTrue();

            request.setServletPath("/api/sandbox/resume");
            assertThat(interceptor.shouldNotFilter(request)).isTrue();

            request.setServletPath("/api/sandbox/convert");
            assertThat(interceptor.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("Skips /api/auth/** paths")
        void skips_authPaths() {
            request.setServletPath("/api/auth/login");
            assertThat(interceptor.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("Skips /api/mock/** paths")
        void skips_publicMockPaths() {
            request.setServletPath("/api/mock/org/project/schema/records");
            assertThat(interceptor.shouldNotFilter(request)).isTrue();
        }

        @Test
        @DisplayName("Does NOT skip regular API paths")
        void doesNotSkip_regularApiPaths() {
            request.setServletPath("/api/organizations");
            assertThat(interceptor.shouldNotFilter(request)).isFalse();

            request.setServletPath("/api/my-org/my-project/schemas");
            assertThat(interceptor.shouldNotFilter(request)).isFalse();
        }
    }

    // =========================================================================
    // GUEST user — sandbox token validation
    // =========================================================================

    @Nested
    @DisplayName("GUEST user validation")
    class GuestValidation {

        @BeforeEach
        void setGuestAuth() {
            setAuthentication("ROLE_GUEST");
            request.setServletPath("/api/organizations");
            request.setRequestURI("/api/organizations");
        }

        @Test
        @DisplayName("Missing sandbox_token cookie returns HTTP 410 with SANDBOX_EXPIRED")
        void missingCookie_returnsSandboxExpired() throws Exception {
            // No cookies added to request

            interceptor.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(410);
            assertThat(response.getContentType()).contains("application/json");

            String body = response.getContentAsString();
            assertThat(body).contains("SANDBOX_EXPIRED");
            assertThat(body).contains("/api/sandbox/convert");

            verify(filterChain, never()).doFilter(any(), any());
            verifyNoInteractions(sandboxService);
        }

        @Test
        @DisplayName("Unknown sandbox token (Redis miss) returns HTTP 410")
        void unknownToken_returnsSandboxExpired() throws Exception {
            addSandboxCookie("unknown-token-not-in-redis");
            when(sandboxService.getSession("unknown-token-not-in-redis"))
                    .thenReturn(Optional.empty());

            interceptor.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(410);
            assertThat(response.getContentAsString()).contains("SANDBOX_EXPIRED");
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("Hard-expired session returns HTTP 410")
        void hardExpiredSession_returnsSandboxExpired() throws Exception {
            addSandboxCookie("expired-token");

            SandboxSession expiredSession = SandboxSession.builder()
                    .userId(UUID.randomUUID())
                    .createdAtEpochMs(System.currentTimeMillis() - 90_000_000L)
                    .hardExpiresAtEpochMs(System.currentTimeMillis() - 1_000L)
                    .build();

            when(sandboxService.getSession("expired-token"))
                    .thenReturn(Optional.of(expiredSession));

            interceptor.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(410);
            verify(filterChain, never()).doFilter(any(), any());
            verify(sandboxService, never()).rollTtl(anyString(), any());
        }

        @Test
        @DisplayName("Valid session passes through and rolls TTL")
        void validSession_passesThroughAndRollsTtl() throws Exception {
            addSandboxCookie("valid-token");
            when(sandboxService.getSession("valid-token"))
                    .thenReturn(Optional.of(activeSession));

            interceptor.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            verify(filterChain).doFilter(request, response);
            verify(sandboxService).rollTtl("valid-token", activeSession);
        }

        @Test
        @DisplayName("Valid session does not modify the response body")
        void validSession_doesNotWriteBody() throws Exception {
            addSandboxCookie("valid-token");
            when(sandboxService.getSession("valid-token"))
                    .thenReturn(Optional.of(activeSession));

            interceptor.doFilterInternal(request, response, filterChain);

            assertThat(response.getContentAsString()).isEmpty();
        }

        @Test
        @DisplayName("Error response contains correct JSON fields")
        void expiredResponse_hasCorrectJsonStructure() throws Exception {
            addSandboxCookie("expired-token");
            when(sandboxService.getSession("expired-token"))
                    .thenReturn(Optional.empty());
            request.setServletPath("/api/organizations");

            interceptor.doFilterInternal(request, response, filterChain);

            String json = response.getContentAsString();
            assertThat(json)
                    .contains("\"status\":410")
                    .contains("\"errorCode\":\"SANDBOX_EXPIRED\"")
                    .contains("\"actionUrl\":\"/api/sandbox/convert\"")
                    .contains("\"path\":\"/api/organizations\"");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void setAuthentication(String role) {
        var principal = new User("test-user", "", List.of(new SimpleGrantedAuthority(role)));
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void addSandboxCookie(String value) {
        request.setCookies(new MockCookie(SandboxRequestInterceptor.SANDBOX_COOKIE_NAME, value));
    }
}