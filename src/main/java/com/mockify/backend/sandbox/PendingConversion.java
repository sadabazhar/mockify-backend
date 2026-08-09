package com.mockify.backend.sandbox;

import lombok.*;

import java.util.UUID;

/**
 * Stored in Redis under key: sandbox:convert:{verificationToken}
 * TTL: 15 minutes
 *
 * Created by initiateSandboxConversion().
 * Consumed atomically by completeSandboxConversion().
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingConversion {

    private UUID userId;
    private String email;
    private String encodedPassword;

    /**
     * The original sandbox session token.
     * Consumed (deleted from Redis) once conversion completes,
     * preventing any future resume attempts on the converted account.
     */
    private String sandboxToken;
}