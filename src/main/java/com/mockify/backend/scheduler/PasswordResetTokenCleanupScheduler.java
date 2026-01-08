package com.mockify.backend.scheduler;

import com.mockify.backend.service.PasswordResetTokenCleanupService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetTokenCleanupScheduler {

    private final PasswordResetTokenCleanupService cleanupService;

    @Value("${cleanup.password-token.enabled:true}")
    private boolean enabled;

    @Scheduled(cron = "${cleanup.password-token.cron}")
    public void cleanExpiredPasswordTokens() {
        if (!enabled) {
            return;
        }

        try {
            long start = System.currentTimeMillis();
            int deleted = cleanupService.cleanExpiredTokens();
            long duration = System.currentTimeMillis() - start;

            log.info("[Cleanup] Deleted {} expired password reset tokens in {} ms", deleted, duration);
        } catch (Exception ex) {
            log.error("[Cleanup] Password token cleanup failed", ex);
        }
    }

    // For Debugging
    @PostConstruct
    public void init() {
        log.info("PasswordResetTokenCleanupScheduler initialized");
    }
}
