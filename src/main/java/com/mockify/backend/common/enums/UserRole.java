package com.mockify.backend.common.enums;

public enum UserRole {
    USER,
    ADMIN,

    /**
     * Ephemeral guest role for sandbox sessions.
     *
     * A GUEST user:
     * - has no email, no password, no provider
     * - cannot log in via /api/auth/login
     * - cannot request a password reset
     * - owns exactly one sandbox organization
     * - is automatically deleted when the sandbox org expires
     *
     * Code that checks for authorization should treat GUEST
     * identically to USER for resource-level access
     * (the sandbox org IS their org — full ownership applies).
     * Code that manages account identity (email, password, login)
     * must explicitly exclude GUEST.
     */
    GUEST
}