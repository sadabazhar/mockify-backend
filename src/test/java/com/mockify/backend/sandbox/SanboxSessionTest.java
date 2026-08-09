package com.mockify.backend.sandbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SandboxSession domain logic")
class SandboxSessionTest {

    @Nested
    @DisplayName("isHardExpired()")
    class HardExpired {

        @Test
        @DisplayName("Not expired when hard cap is in the future")
        void notExpired_whenHardCapInFuture() {
            SandboxSession session = SandboxSession.builder()
                    .userId(UUID.randomUUID())
                    .createdAtEpochMs(System.currentTimeMillis() - 1000)
                    .hardExpiresAtEpochMs(System.currentTimeMillis() + 60_000)
                    .build();

            assertThat(session.isHardExpired()).isFalse();
        }

        @Test
        @DisplayName("Expired when hard cap is in the past")
        void expired_whenHardCapInPast() {
            SandboxSession session = SandboxSession.builder()
                    .userId(UUID.randomUUID())
                    .createdAtEpochMs(System.currentTimeMillis() - 90_000)
                    .hardExpiresAtEpochMs(System.currentTimeMillis() - 1_000)
                    .build();

            assertThat(session.isHardExpired()).isTrue();
        }
    }

    @Nested
    @DisplayName("remainingMs()")
    class RemainingMs {

        @Test
        @DisplayName("Returns positive value for active session")
        void remainingIsPositive_forActiveSession() {
            SandboxSession session = SandboxSession.builder()
                    .hardExpiresAtEpochMs(System.currentTimeMillis() + 10_000)
                    .build();

            assertThat(session.remainingMs()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Returns zero (not negative) for expired session")
        void remainingIsZero_forExpiredSession() {
            SandboxSession session = SandboxSession.builder()
                    .hardExpiresAtEpochMs(System.currentTimeMillis() - 5_000)
                    .build();

            assertThat(session.remainingMs()).isEqualTo(0);
        }
    }
}