package com.mockify.backend.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class ApiKeyEntityTest {

    @Test
    void testApiKeyEntityCreation() {
        ApiKey key = ApiKey.builder()
                .name("Test Key")
                .keyPrefix("mk_live_")
                .keyHash("test_hash")
                .isActive(true)
                .rateLimitPerMinute(1000)
                .build();

        assertNotNull(key);
        assertEquals("Test Key", key.getName());
        assertTrue(key.isActive());
    }

    @Test
    void testApiKeyValidation_Active() {
        ApiKey key = ApiKey.builder()
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        assertTrue(key.isValid());
    }

    @Test
    void testApiKeyValidation_Expired() {
        ApiKey key = ApiKey.builder()
                .isActive(true)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        assertFalse(key.isValid());
    }

    @Test
    void testApiKeyValidation_Inactive() {
        ApiKey key = ApiKey.builder()
                .isActive(false)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        assertFalse(key.isValid());
    }

    @Test
    void testPermissionHierarchy() {
        // ADMIN includes DELETE
        assertTrue(ApiKeyPermission.ApiPermission.ADMIN
                .includes(ApiKeyPermission.ApiPermission.DELETE));

        // DELETE includes WRITE
        assertTrue(ApiKeyPermission.ApiPermission.DELETE
                .includes(ApiKeyPermission.ApiPermission.WRITE));

        // WRITE includes READ
        assertTrue(ApiKeyPermission.ApiPermission.WRITE
                .includes(ApiKeyPermission.ApiPermission.READ));

        // READ does not include WRITE
        assertFalse(ApiKeyPermission.ApiPermission.READ
                .includes(ApiKeyPermission.ApiPermission.WRITE));
    }
}