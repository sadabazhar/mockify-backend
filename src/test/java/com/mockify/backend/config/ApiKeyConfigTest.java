package com.mockify.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiKeyConfigTest {

    @Value("${app.api-key.secret}")
    private String apiKeySecret;

    @Value("${app.api-key.max-per-organization}")
    private Integer maxPerOrganization;

    @Test
    void testApiKeySecretLoaded() {
        assertNotNull(apiKeySecret, "API key secret should be loaded");
        assertFalse(apiKeySecret.isEmpty(), "API key secret should not be empty");
        System.out.println("API Key Secret loaded (length): " + apiKeySecret.length());
    }

    @Test
    void testMaxPerOrganizationLoaded() {
        assertNotNull(maxPerOrganization, "Max per org should be loaded");
        assertTrue(maxPerOrganization > 0, "Max per org should be positive");
        System.out.println("Max keys per org: " + maxPerOrganization);
    }

    @Test
    void testSecretMinimumLength() {
        assertTrue(apiKeySecret.length() >= 32,
                "API key secret should be at least 32 characters for security");
    }
}