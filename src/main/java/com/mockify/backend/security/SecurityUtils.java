package com.mockify.backend.security;

import com.mockify.backend.exception.AccessDeniedException;
import com.mockify.backend.exception.ForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

/**
 * Utility methods for authentication type enforcement.
 *
 * Use {@link #requireJwtAuthentication(Authentication)} on any endpoint that must
 * not be reachable via an API key — e.g. API key management, org/project lifecycle.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    /**
     * Throws {@link ForbiddenException} if the current request is authenticated
     * via an API key rather than a JWT session.
     *
     * <p>Call this at the top of any controller method that must remain
     * JWT-only (account management, API key CRUD, org/project destructive ops).</p>
     */
    public static void requireJwtAuthentication(Authentication auth) {
        if (auth instanceof ApiKeyAuthenticationToken) {
            throw new ForbiddenException(
                    "This operation requires session-based (JWT) authentication. " +
                            "API key authentication is not permitted here."
            );
        }
    }

    /**
     * Resolves the authenticated user's UUID from either a JWT or API key token.
     * Always call {@link #requireJwtAuthentication} first on JWT-only endpoints.
     */
    public static UUID resolveUserId(Authentication auth) {
        if (auth instanceof ApiKeyAuthenticationToken token) {
            return token.getOwnerId();
        }
        if (auth.getPrincipal() instanceof UserDetails user) {
            try {
                return UUID.fromString(user.getUsername());
            } catch (IllegalArgumentException ex) {
                throw new AccessDeniedException(
                        "Invalid user identifier in authentication token " + ex
                );
            }
        }

        throw new AccessDeniedException(
                "Unknown authentication type"
        );
    }
}