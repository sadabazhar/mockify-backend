package com.mockify.backend.service;

import com.mockify.backend.dto.request.apikey.CreateApiKeyRequest;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.model.*;
import com.mockify.backend.model.ApiKeyPermission.*;
import com.mockify.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiKeyServiceLimitTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Value("${app.api-key.max-per-organization}")
    private int maxKeysPerOrg;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Limit Test User");
        testUser.setEmail("limit-test@example.com");
        testUser.setPassword("hashed_password");
        testUser.setProviderName("local");
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        testOrg = new Organization();
        testOrg.setName("Limit Test Org");
        testOrg.setSlug("limit-test-org");
        testOrg.setOwner(testUser);
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    void testCreateApiKey_EnforcesLimit() {
        // Create keys up to the limit
        for (int i = 0; i < maxKeysPerOrg; i++) {
            CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                    .name("Key " + i)
                    .permissions(List.of(
                            new CreateApiKeyRequest.PermissionRequest(
                                    ApiPermission.READ,
                                    ApiResourceType.RECORD,
                                    null
                            )
                    ))
                    .build();

            apiKeyService.createApiKey(testUser.getId(), testOrg.getId(), request);
        }

        // Attempt to create one more - should fail
        CreateApiKeyRequest extraRequest = CreateApiKeyRequest.builder()
                .name("Extra Key")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> apiKeyService.createApiKey(
                        testUser.getId(),
                        testOrg.getId(),
                        extraRequest
                )
        );

        assertTrue(exception.getMessage().contains("Maximum number of API keys"));
    }

    @Test
    void testCreateApiKey_LimitOnlyCountsActiveKeys() {
        // Create max keys
        for (int i = 0; i < maxKeysPerOrg; i++) {
            CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                    .name("Key " + i)
                    .permissions(List.of(
                            new CreateApiKeyRequest.PermissionRequest(
                                    ApiPermission.READ,
                                    ApiResourceType.RECORD,
                                    null
                            )
                    ))
                    .build();

            apiKeyService.createApiKey(testUser.getId(), testOrg.getId(), request);
        }

        // Get one key and revoke it
        var keys = apiKeyService.listOrganizationKeys(testUser.getId(), testOrg.getId());
        apiKeyService.revokeApiKey(testUser.getId(), testOrg.getId(), keys.get(0).getId());

        // Should now be able to create one more
        CreateApiKeyRequest newRequest = CreateApiKeyRequest.builder()
                .name("New Key After Revocation")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        assertDoesNotThrow(() -> {
            apiKeyService.createApiKey(testUser.getId(), testOrg.getId(), newRequest);
        });
    }
}