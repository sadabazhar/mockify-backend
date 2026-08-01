package com.mockify.backend.service;

import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrentSchemaManager {

    private final MockSchemaService mockSchemaService;

    @Value("${app.concurrency.max-retries:3}")
    private int maxRetries;

    @Value("${app.concurrency.initial-delay-ms:100}")
    private long initialDelayMs;

    @Value("${app.concurrency.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    /**
     * Safely updates a schema using optimistic locking.
     * Automatically retries when concurrent update conflicts occur.
     */
    public MockSchemaResponse updateSchemaWithRetry(
            UUID userId,
            UUID schemaId,
            UpdateMockSchemaRequest request
    ) {

        long delay = initialDelayMs;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {

            try {
                return mockSchemaService.updateSchema(userId, schemaId, request);

            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException ex) {

                if (attempt == maxRetries) {
                    log.error(
                            "Schema update failed after {} attempts. schemaId={}, userId={}",
                            maxRetries,
                            schemaId,
                            userId,
                            ex
                    );
                    throw ex;
                }

                // Add random jitter (0-50ms) to reduce retry collisions
                long jitter = ThreadLocalRandom.current().nextLong(0, 51);
                long waitTime = delay + jitter;

                log.warn(
                        "Optimistic lock conflict while updating schema {} (Attempt {}/{}). Retrying in {} ms.",
                        schemaId,
                        attempt,
                        maxRetries,
                        waitTime
                );

                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Retry interrupted while updating schema " + schemaId,
                            interruptedException
                    );
                }

                delay = (long) (delay * backoffMultiplier);
            }
        }

        throw new IllegalStateException("Unexpected retry termination.");
    }
}