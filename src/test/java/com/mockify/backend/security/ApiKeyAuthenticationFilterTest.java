package com.mockify.backend.security;

import com.mockify.backend.model.*;
import com.mockify.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiKeyAuthenticationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiKeyCryptoService cryptoService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Value("${app.api-key.secret}")
    private String globalSecret;

    private User testUser;
    private Organization testOrg;
    private String validApiKey;
    private String validKeyHash;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setName("Filter Test User");
        testUser.setEmail("filter-test@example.com");
        testUser.setPassword("hashed_password");
        testUser.setProviderName("local");
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        // Create test organization
        testOrg = new Organization();
        testOrg.setName("Filter Test Org");
        testOrg.setSlug("filter-test-org");
        testOrg.setOwner(testUser);
        testOrg = organizationRepository.save(testOrg);

        // Generate API key
        validApiKey = cryptoService.generateApiKey(false);
        String orgSecret = cryptoService.generateOrgSecret(
                testOrg.getId().toString(),
                globalSecret
        );
        validKeyHash = cryptoService.hashApiKey(validApiKey, orgSecret);

        // Save API key in database
        ApiKey apiKey = ApiKey.builder()
                .name("Test Key")
                .keyPrefix(cryptoService.extractKeyPrefix(validApiKey))
                .keyHash(validKeyHash)
                .organization(testOrg)
                .createdBy(testUser)
                .isActive(true)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .rateLimitPerMinute(1000)
                .build();
        apiKeyRepository.save(apiKey);
    }

    @Test
    void testAuthenticationWithXApiKeyHeader() throws Exception {
        mockMvc.perform(get("/api/organizations")
                        .header("X-API-Key", validApiKey))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthenticationWithAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/organizations")
                        .header("Authorization", "ApiKey " + validApiKey))
                .andExpect(status().isOk());
    }

    @Test
    void testAuthenticationWithInvalidKey() throws Exception {
        String invalidKey = "mk_live_invalid_key_12345678901234567890";

        mockMvc.perform(get("/api/organizations")
                        .header("X-API-Key", invalidKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticationWithInvalidFormat() throws Exception {
        String malformedKey = "invalid-format-key";

        mockMvc.perform(get("/api/organizations")
                        .header("X-API-Key", malformedKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticationWithExpiredKey() throws Exception {
        // Create expired key
        String expiredApiKey = cryptoService.generateApiKey(false);
        String orgSecret = cryptoService.generateOrgSecret(
                testOrg.getId().toString(),
                globalSecret
        );
        String expiredKeyHash = cryptoService.hashApiKey(expiredApiKey, orgSecret);

        ApiKey expiredKey = ApiKey.builder()
                .name("Expired Key")
                .keyPrefix(cryptoService.extractKeyPrefix(expiredApiKey))
                .keyHash(expiredKeyHash)
                .organization(testOrg)
                .createdBy(testUser)
                .isActive(true)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
                .rateLimitPerMinute(1000)
                .build();
        apiKeyRepository.save(expiredKey);

        mockMvc.perform(get("/api/organizations")
                        .header("X-API-Key", expiredApiKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticationWithInactiveKey() throws Exception {
        // Create inactive key
        String inactiveApiKey = cryptoService.generateApiKey(false);
        String orgSecret = cryptoService.generateOrgSecret(
                testOrg.getId().toString(),
                globalSecret
        );
        String inactiveKeyHash = cryptoService.hashApiKey(inactiveApiKey, orgSecret);

        ApiKey inactiveKey = ApiKey.builder()
                .name("Inactive Key")
                .keyPrefix(cryptoService.extractKeyPrefix(inactiveApiKey))
                .keyHash(inactiveKeyHash)
                .organization(testOrg)
                .createdBy(testUser)
                .isActive(false) // Inactive
                .rateLimitPerMinute(1000)
                .build();
        apiKeyRepository.save(inactiveKey);

        mockMvc.perform(get("/api/organizations")
                        .header("X-API-Key", inactiveApiKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAuthenticationWithNoHeader() throws Exception {
        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testPublicEndpointsDoNotRequireApiKey() throws Exception {
        // These endpoints should be accessible without API key
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/endpoints/lookup/some-slug"))
                .andExpect(status().isOk());
    }
}