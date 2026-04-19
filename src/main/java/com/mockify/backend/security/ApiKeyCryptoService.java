package com.mockify.backend.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Cryptographic service for API key generation and hashing
 * Uses HMAC-SHA256 for secure, deterministic key hashing
 */
@Service
@Slf4j
public class ApiKeyCryptoService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern KEY_PATTERN =
            Pattern.compile("^(mk_live_|mk_test_)[A-Za-z0-9_-]{43}$");

    // Key format: mk_live_<32 bytes base64url>
    private static final String KEY_PREFIX_LIVE = "mk_live_";
    private static final String KEY_PREFIX_TEST = "mk_test_";
    private static final int KEY_ENTROPY_BYTES = 32; // 256 bits

    /**
     * Generate a new API key with cryptographically secure randomness
     *
     * @param isTestMode true for test keys (mk_test_), false for live keys (mk_live_)
     * @return newly generated API key string
     */
    public String generateApiKey(boolean isTestMode) {
        byte[] randomBytes = new byte[KEY_ENTROPY_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);

        // Use base64url encoding (URL-safe, no padding)
        String encodedKey = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String prefix = isTestMode ? KEY_PREFIX_TEST : KEY_PREFIX_LIVE;
        return prefix + encodedKey;
    }

    /**
     * Generate HMAC-SHA256 hash of API key
     * This is what gets stored in the database
     *
     * @param apiKey the raw API key to hash
     * @param secret HMAC secret (should be unique per organization for additional security)
     * @return hex-encoded HMAC-SHA256 hash
     */
    public String hashApiKey(String apiKey, String secret) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key must not be null or blank");
        }
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Secret must not be null or blank");
        }

        try {
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            hmac.init(secretKey);

            byte[] hashBytes = hmac.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));

            // Return hex-encoded hash
            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to hash API key", e);
            throw new IllegalStateException("Cryptographic error during key hashing", e);
        }
    }

    /**
     * Verify API key against stored hash
     *
     * @param apiKey the raw API key to verify
     * @param storedHash the hash stored in database
     * @param secret HMAC secret used during hashing
     * @return true if key matches hash
     */
    public boolean verifyApiKey(String apiKey, String storedHash, String secret) {
        if (storedHash == null || secret == null) {
            return false;
        }

        if (!isValidKeyFormat(apiKey)) {
            return false;
        }

        String computedHash = hashApiKey(apiKey, secret);

        // Use constant-time comparison to prevent timing attacks
        return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Extract visible prefix from API key
     * Used for displaying keys to users (mk_live_****)
     *
     * @param apiKey full API key
     * @return prefix + first 4 chars of key
     */
    public String extractKeyPrefix(String apiKey) {
        if (!isValidKeyFormat(apiKey)) {
            throw new IllegalArgumentException("Invalid API key format");
        }

        int prefixLength = apiKey.startsWith(KEY_PREFIX_LIVE)
                ? KEY_PREFIX_LIVE.length()
                : KEY_PREFIX_TEST.length();

        return apiKey.substring(0, prefixLength + 4);
    }

    /**
     * Validate API key format
     *
     * @param apiKey key to validate
     * @return true if format is valid
     */
    public boolean isValidKeyFormat(String apiKey) {
        if (apiKey == null) return false;
        return KEY_PATTERN.matcher(apiKey).matches();
    }

    /**
     * Generate organization-specific HMAC secret
     * In production, this should be stored securely
     * For now, we derive it from organization ID
     *
     * @param organizationId organization UUID
     * @param globalSecret application-level secret
     * @return organization-specific secret
     */
    public String generateOrgSecret(String organizationId, String globalSecret) {
        if (organizationId == null || organizationId.isBlank()) {
            throw new IllegalArgumentException("Organization ID must not be null or blank");
        }
        if (globalSecret == null || globalSecret.isBlank()) {
            throw new IllegalArgumentException("Global secret must not be null or blank");
        }

        String input = organizationId + ":api-key";
        return hashApiKey(input, globalSecret);
    }
}