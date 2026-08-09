package com.mockify.backend.sandbox;

import lombok.*;

import java.util.UUID;

/**
 * Stored in Redis under key: sandbox:session:{rawToken}
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SandboxSession {

    private UUID userId;
    private UUID organizationId;

    /** Epoch ms when the sandbox was first created. Never changes. */
    private long createdAtEpochMs;

    /**
     * Epoch ms of the absolute maximum lifetime.
     * Rolling TTL cannot push beyond this cap.
     * Equals createdAtEpochMs + 24 hours.
     */
    private long hardExpiresAtEpochMs;

    // -------------------------------------------------------------------------
    // Domain helpers
    // -------------------------------------------------------------------------

    public boolean isHardExpired() {
        return System.currentTimeMillis() > hardExpiresAtEpochMs;
    }

    /**
     * Remaining milliseconds until hard expiry.
     * Returns 0 if already expired (never negative).
     */
    public long remainingMs() {
        return Math.max(0, hardExpiresAtEpochMs - System.currentTimeMillis());
    }
}