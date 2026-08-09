package com.mockify.backend.service;

import com.mockify.backend.dto.response.auth.AuthResult;
import com.mockify.backend.dto.response.sandbox.SandboxCreationResult;
import com.mockify.backend.dto.response.sandbox.SandboxResumeResult;
import com.mockify.backend.sandbox.SandboxSession;

import java.util.Optional;

public interface SandboxService {

    /**
     * Creates a fully provisioned sandbox workspace for a new guest user.
     * Provisions: guest user → org → project → 2 schemas → seeded records.
     * Stores session in Redis and hashes token in DB as fallback.
     *
     * @param clientIp used for logging and potential abuse detection
     */
    SandboxCreationResult createSandbox(String clientIp);

    /**
     * Validates a sandbox token and issues a fresh JWT.
     * Rolls the Redis TTL forward (up to the hard cap).
     * Called when the short-lived JWT expires but the session is still valid.
     *
     * @throws com.mockify.backend.exception.SandboxExpiredException if hard cap exceeded
     * @throws com.mockify.backend.exception.ResourceNotFoundException if token unknown
     */
    SandboxResumeResult resumeSandbox(String sandboxToken);

    /**
     * Starts the email-based conversion flow.
     * Validates the sandbox token, checks email availability,
     * stores a PendingConversion in Redis, and sends a verification email.
     *
     * @throws com.mockify.backend.exception.SandboxExpiredException if session expired
     * @throws com.mockify.backend.exception.DuplicateResourceException if email taken
     */
    void initiateSandboxConversion(String sandboxToken, String email, String password);

    /**
     * Completes conversion by consuming the email verification token.
     * In a single transaction: upgrades the guest user to a real account
     * and promotes the sandbox org to a permanent org.
     * Invalidates the sandbox session in Redis.
     *
     * @return a full auth session (access token + refresh cookie) ready to return
     */
    AuthResult completeSandboxConversion(String verificationToken);

    /**
     * Retrieves the session object for a given sandbox token.
     * Used by the request interceptor to validate and roll TTL.
     */
    Optional<SandboxSession> getSession(String sandboxToken);

    /**
     * Rolls the session TTL forward.
     * Caps at hardExpiresAt — no extension beyond the absolute limit.
     */
    void rollTtl(String sandboxToken, SandboxSession session);
}