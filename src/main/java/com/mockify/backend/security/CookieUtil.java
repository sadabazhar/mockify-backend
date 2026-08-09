package com.mockify.backend.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.cookie.refresh-token")
@Component
@Getter
@Setter
public class CookieUtil {

    @NotBlank
    private String name;
    @NotBlank
    private String path;
    @Min(1)
    private long maxAge;
    private boolean secure;
    @NotBlank
    private String sameSite;

    // ── Sandbox token cookie config (bound from app.cookie.sandbox-token.*) ──
    // These are set programmatically from SandboxServiceImpl
    private String sandboxCookieName   = "sandbox_token";
    private String sandboxCookiePath   = "/api/sandbox";
    private long   sandboxCookieMaxAge = 86400L;  // 24h default

    // =========================================================================
    // Refresh token
    // =========================================================================

    public ResponseCookie createRefreshToken(String token) {
        return ResponseCookie.from(name, token)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(maxAge)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie clearRefreshToken() {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }

    // =========================================================================
    // Sandbox token
    // =========================================================================

    /**
     * Creates the long-lived HttpOnly cookie that carries the sandbox
     * session token. Scoped to /api/sandbox so it is only sent on
     * sandbox-specific endpoints (resume, convert), not on every API call.
     */
    public ResponseCookie createSandboxTokenCookie(String rawToken) {
        return ResponseCookie.from(sandboxCookieName, rawToken)
                .httpOnly(true)
                .secure(secure)
                .path(sandboxCookiePath)
                .maxAge(sandboxCookieMaxAge)
                .sameSite(sameSite)
                .build();
    }

    public ResponseCookie clearSandboxTokenCookie() {
        return ResponseCookie.from(sandboxCookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path(sandboxCookiePath)
                .maxAge(0)
                .sameSite(sameSite)
                .build();
    }
}