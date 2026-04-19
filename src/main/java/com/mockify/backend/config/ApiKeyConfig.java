package com.mockify.backend.config;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Configuration properties for API Key system
 * Loaded from application.yml under app.api-key prefix
 */
@Configuration
@ConfigurationProperties(prefix = "app.api-key")
@Validated
@Getter
@Setter
public class ApiKeyConfig {

    /**
     * Global HMAC secret for key hashing
     * MUST be at least 32 characters for security
     * Should be different in each environment
     */
    @NotBlank(message = "API key secret must be configured")
    @Size(min = 32, message = "API key secret must be at least 32 characters")
    private String secret;

    /**
     * Maximum number of API keys allowed per organization
     * Prevents abuse and resource exhaustion
     */
    @Min(value = 1, message = "Max keys per organization must be at least 1")
    private int maxPerOrganization = 10;

    /**
     * Enable/disable API key authentication globally
     * Useful for debugging or emergency shutdown
     */
    private boolean enabled = true;

    /**
     * Default rate limit for new API keys (requests per minute)
     */
    @Min(value = 1, message = "Default rate limit must be at least 1")
    private int defaultRateLimit = 1000;

    /**
     * Maximum rate limit allowed for any API key
     */
    @Min(value = 1, message = "Max rate limit must be at least 1")
    private int maxRateLimit = 100000;
}