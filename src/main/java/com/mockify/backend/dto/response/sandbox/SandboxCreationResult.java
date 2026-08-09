package com.mockify.backend.dto.response.sandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseCookie;

@Getter
@AllArgsConstructor
@Builder
public class SandboxCreationResult {

    /** Short-lived JWT for immediate API access (1 hour). */
    private String accessToken;

    /** Token expiry in milliseconds, matching JWT_ACCESS_EXPIRATION. */
    private long expiresIn;

    /**
     * HttpOnly cookie carrying the long-lived sandbox session token.
     * The controller sets this as a response header.
     * Not serialized in the JSON response body.
     */
    private transient ResponseCookie sandboxCookie;

    /** Describes the auto-provisioned workspace. */
    private SandboxWorkspace workspace;
}