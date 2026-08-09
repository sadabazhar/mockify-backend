package com.mockify.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.dto.response.error.ErrorResponse;
import com.mockify.backend.sandbox.SandboxSession;
import com.mockify.backend.service.SandboxService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Validates the sandbox session on every request made by a GUEST user.
 *
 * <h3>Responsibility</h3>
 * <ul>
 *   <li>Read {@code sandbox_token} from the HttpOnly cookie.</li>
 *   <li>Look up the session in Redis via {@link SandboxService#getSession}.</li>
 *   <li>Enforce the hard expiry cap — block the request with HTTP 410 if exceeded.</li>
 *   <li>Roll the Redis TTL forward on every successful request (activity-based extension).</li>
 * </ul>
 *
 * <h3>Filter position in the chain</h3>
 * Runs after {@link ApiKeyRateLimitFilter} — authentication and rate limiting
 * both happen before session validation so a GUEST user who exceeds their rate
 * limit gets a 429, not a 410, which is the correct response ordering.
 *
 * <h3>Pass-through conditions</h3>
 * <ol>
 *   <li>Path matches {@link #SKIP_PATH_PATTERNS} — filter short-circuits immediately
 *       via {@link #shouldNotFilter}.</li>
 *   <li>Authenticated principal does not carry {@code ROLE_GUEST} — real users and
 *       API key callers pass through without any Redis lookup.</li>
 * </ol>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SandboxRequestInterceptor extends OncePerRequestFilter {

    static final String SANDBOX_COOKIE_NAME = "sandbox_token";

    private static final String EXPIRED_ERROR_CODE = "SANDBOX_EXPIRED";
    private static final String EXPIRED_MESSAGE =
            "Your sandbox session has expired. Convert your account to keep your work.";
    private static final String CONVERT_URL = "/api/sandbox/convert";

    /**
     * Paths skipped entirely — the filter body never runs for these.
     * Sandbox-specific endpoints (/api/sandbox/**) handle their own
     * token validation; skipping them here avoids redundant Redis lookups.
     */
    private static final List<String> SKIP_PATH_PATTERNS = List.of(
            "/api/sandbox/**",
            "/api/auth/**",
            "/api/mock/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/webjars/**",
            "/actuator/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/.well-known/**"
    );

    private final SandboxService sandboxService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // =========================================================================
    // Filter entry point
    // =========================================================================

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Non-guest callers (real users, API keys, unauthenticated) pass through
        if (!isGuestAuthentication(auth)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Guest user — sandbox session must be present and valid
        Optional<String> tokenOpt = extractSandboxToken(request);

        if (tokenOpt.isEmpty()) {
            log.warn("GUEST request with missing sandbox_token cookie: uri={}",
                    request.getRequestURI());
            sendExpiredResponse(request, response);
            return;
        }

        String sandboxToken = tokenOpt.get();
        Optional<SandboxSession> sessionOpt = sandboxService.getSession(sandboxToken);

        if (sessionOpt.isEmpty()) {
            log.info("Sandbox session not found (expired or invalid): uri={}",
                    request.getRequestURI());
            sendExpiredResponse(request, response);
            return;
        }

        SandboxSession session = sessionOpt.get();

        if (session.isHardExpired()) {
            log.info("Sandbox hard cap exceeded: userId={}, uri={}",
                    session.getUserId(), request.getRequestURI());
            sendExpiredResponse(request, response);
            return;
        }

        // Session is valid — roll the TTL and let the request through
        sandboxService.rollTtl(sandboxToken, session);

        log.debug("Sandbox session validated: userId={}, remainingMs={}",
                session.getUserId(), session.remainingMs());

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return SKIP_PATH_PATTERNS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns true only when the current principal carries {@code ROLE_GUEST}.
     * API key callers carry {@code ROLE_API_KEY}, real users carry {@code ROLE_USER}
     * or {@code ROLE_ADMIN} — none of those should be intercepted here.
     */
    private boolean isGuestAuthentication(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_GUEST".equals(a.getAuthority()));
    }

    private Optional<String> extractSandboxToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> SANDBOX_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    private void sendExpiredResponse(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.GONE.value())
                .error("Sandbox Expired")
                .message(EXPIRED_MESSAGE)
                .path(request.getRequestURI())
                .errorCode(EXPIRED_ERROR_CODE)
                .actionUrl(CONVERT_URL)
                .build();

        response.setStatus(HttpStatus.GONE.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}