package com.mockify.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyCryptoServiceTest {

    private ApiKeyCryptoService cryptoService;
    private static final String TEST_SECRET = "test-secret-for-hashing";

    @BeforeEach
    void setUp() {
        cryptoService = new ApiKeyCryptoService();
    }

    // -------------------------------
    // API Key Generation Tests
    // -------------------------------

    @Test
    void testGenerateApiKey_LiveMode() {
        String key = cryptoService.generateApiKey(false);

        assertNotNull(key);
        assertTrue(key.startsWith("mk_live_"));
        assertEquals(51, key.length()); // mk_live_ (8) + 43 chars
    }

    @Test
    void testGenerateApiKey_TestMode() {
        String key = cryptoService.generateApiKey(true);

        assertNotNull(key);
        assertTrue(key.startsWith("mk_test_"));
        assertEquals(51, key.length()); // mk_test_ (8) + 43 chars
    }

    @Test
    void testGenerateApiKey_Uniqueness() {
        String key1 = cryptoService.generateApiKey(false);
        String key2 = cryptoService.generateApiKey(false);

        assertNotEquals(key1, key2);
    }

    @Test
    void testGeneratedApiKey_ValidFormat() {
        String key = cryptoService.generateApiKey(false);
        assertTrue(cryptoService.isValidKeyFormat(key));
    }

    // -------------------------------
    // Hashing Tests
    // -------------------------------

    @Test
    void testHashApiKey_Deterministic() {
        String apiKey = "mk_live_" + "a".repeat(43);

        String hash1 = cryptoService.hashApiKey(apiKey, TEST_SECRET);
        String hash2 = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 = 64 hex chars
    }

    @Test
    void testHashApiKey_DifferentSecrets() {
        String apiKey = "mk_live_" + "a".repeat(43);

        String hash1 = cryptoService.hashApiKey(apiKey, "secret1");
        String hash2 = cryptoService.hashApiKey(apiKey, "secret2");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testHashApiKey_DifferentKeys() {
        String key1 = "mk_live_" + "a".repeat(43);
        String key2 = "mk_live_" + "b".repeat(43);

        String hash1 = cryptoService.hashApiKey(key1, TEST_SECRET);
        String hash2 = cryptoService.hashApiKey(key2, TEST_SECRET);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testHashApiKey_Length() {
        String apiKey = cryptoService.generateApiKey(false);
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        assertEquals(64, hash.length());
    }

    @Test
    void testHashApiKey_BlankInputs() {
        assertThrows(IllegalArgumentException.class, () ->
                cryptoService.hashApiKey("", TEST_SECRET));

        assertThrows(IllegalArgumentException.class, () ->
                cryptoService.hashApiKey("mk_live_" + "a".repeat(43), ""));
    }

    // -------------------------------
    // Verification Tests
    // -------------------------------

    @Test
    void testVerifyApiKey_Success() {
        String apiKey = cryptoService.generateApiKey(false);
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        assertTrue(cryptoService.verifyApiKey(apiKey, hash, TEST_SECRET));
    }

    @Test
    void testVerifyApiKey_Failure_WrongKey() {
        String apiKey = cryptoService.generateApiKey(false);
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        String wrongKey = cryptoService.generateApiKey(false);

        assertFalse(cryptoService.verifyApiKey(wrongKey, hash, TEST_SECRET));
    }

    @Test
    void testVerifyApiKey_WrongSecretFails() {
        String apiKey = cryptoService.generateApiKey(false);
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        assertFalse(
                cryptoService.verifyApiKey(apiKey, hash, "wrong-secret")
        );
    }

    @Test
    void testVerifyApiKey_InvalidFormatRejected() {
        String validKey = cryptoService.generateApiKey(false);
        String hash = cryptoService.hashApiKey(validKey, TEST_SECRET);

        String invalidKey = "invalid";

        assertFalse(
                cryptoService.verifyApiKey(invalidKey, hash, TEST_SECRET)
        );
    }

    @Test
    void testVerifyApiKey_NullHash() {
        String apiKey = cryptoService.generateApiKey(false);
        assertFalse(cryptoService.verifyApiKey(apiKey, null, TEST_SECRET));
    }

    @Test
    void testVerifyApiKey_NullSecret() {
        String apiKey = cryptoService.generateApiKey(false);
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        assertFalse(cryptoService.verifyApiKey(apiKey, hash, null));
    }

    // -------------------------------
    // Prefix Extraction Tests
    // -------------------------------

    @Test
    void testExtractKeyPrefix_LiveKey() {
        String apiKey = "mk_live_" + "abcd" + "a".repeat(39);
        String prefix = cryptoService.extractKeyPrefix(apiKey);

        assertEquals("mk_live_abcd", prefix);
    }

    @Test
    void testExtractKeyPrefix_TestKey() {
        String apiKey = "mk_test_" + "abcd" + "a".repeat(39);
        String prefix = cryptoService.extractKeyPrefix(apiKey);

        assertEquals("mk_test_abcd", prefix);
    }

    @Test
    void testExtractKeyPrefix_FromGeneratedKey() {
        String apiKey = cryptoService.generateApiKey(false);
        String prefix = cryptoService.extractKeyPrefix(apiKey);

        assertTrue(prefix.startsWith("mk_live_"));
        assertEquals(12, prefix.length()); // mk_live_ + 4 chars
    }

    @Test
    void testExtractKeyPrefix_InvalidKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            cryptoService.extractKeyPrefix("invalid");
        });
    }

    // -------------------------------
    // Format Validation Tests
    // -------------------------------

    @Test
    void testIsValidKeyFormat_LiveKey() {
        String validKey = "mk_live_" + "a".repeat(43);
        assertTrue(cryptoService.isValidKeyFormat(validKey));
    }

    @Test
    void testIsValidKeyFormat_TestKey() {
        String validKey = "mk_test_" + "a".repeat(43);
        assertTrue(cryptoService.isValidKeyFormat(validKey));
    }

    @Test
    void testIsValidKeyFormat_InvalidPrefix() {
        String invalidKey = "invalid_prefix_" + "a".repeat(43);
        assertFalse(cryptoService.isValidKeyFormat(invalidKey));
    }

    @Test
    void testIsValidKeyFormat_InvalidCharacters() {
        String invalidKey = "mk_live_" + "@".repeat(43);
        assertFalse(cryptoService.isValidKeyFormat(invalidKey));
    }

    @Test
    void testIsValidKeyFormat_TooShort() {
        String shortKey = "mk_live_abc";
        assertFalse(cryptoService.isValidKeyFormat(shortKey));
    }

    @Test
    void testIsValidKeyFormat_TooLong() {
        String longKey = "mk_live_" + "a".repeat(100);
        assertFalse(cryptoService.isValidKeyFormat(longKey));
    }

    @Test
    void testIsValidKeyFormat_Null() {
        assertFalse(cryptoService.isValidKeyFormat(null));
    }

    @Test
    void testIsValidKeyFormat_Empty() {
        assertFalse(cryptoService.isValidKeyFormat(""));
    }

    // -------------------------------
    // Organization Secret Tests
    // -------------------------------

    @Test
    void testGenerateOrgSecret_Deterministic() {
        String orgId = "550e8400-e29b-41d4-a716-446655440000";

        String secret1 = cryptoService.generateOrgSecret(orgId, TEST_SECRET);
        String secret2 = cryptoService.generateOrgSecret(orgId, TEST_SECRET);

        assertEquals(secret1, secret2);
    }

    @Test
    void testGenerateOrgSecret_DifferentOrgs() {
        String orgId1 = "550e8400-e29b-41d4-a716-446655440000";
        String orgId2 = "660e8400-e29b-41d4-a716-446655440000";

        String secret1 = cryptoService.generateOrgSecret(orgId1, TEST_SECRET);
        String secret2 = cryptoService.generateOrgSecret(orgId2, TEST_SECRET);

        assertNotEquals(secret1, secret2);
    }

    // -------------------------------
    // End-to-End Workflow
    // -------------------------------

    @Test
    void testFullWorkflow_GenerateHashVerify() {
        // Generate key
        String apiKey = cryptoService.generateApiKey(false);

        // Hash it
        String hash = cryptoService.hashApiKey(apiKey, TEST_SECRET);

        // Verify it
        assertTrue(cryptoService.verifyApiKey(apiKey, hash, TEST_SECRET));

        // Verify wrong key fails
        String wrongKey = cryptoService.generateApiKey(false);
        assertFalse(cryptoService.verifyApiKey(wrongKey, hash, TEST_SECRET));
    }
}
