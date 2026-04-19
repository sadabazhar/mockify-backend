package com.mockify.backend.service;

import com.mockify.backend.dto.request.apikey.CreateApiKeyRequest;
import com.mockify.backend.dto.request.apikey.UpdateApiKeyRequest;
import com.mockify.backend.dto.response.apikey.ApiKeyResponse;
import com.mockify.backend.dto.response.apikey.CreateApiKeyResult;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.model.*;
import com.mockify.backend.model.ApiKeyPermission.*;
import com.mockify.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiKeyServiceIntegrationTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashed_password");
        testUser.setProviderName("local");
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        // Create test organization
        testOrg = new Organization();
        testOrg.setName("Test Org");
        testOrg.setSlug("test-org");
        testOrg.setOwner(testUser);
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    void testCreateApiKey_Success() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test API Key")
                .description("Test Description")
                .rateLimitPerMinute(1000)
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        CreateApiKeyResult result = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                request
        );

        assertNotNull(result);
        assertNotNull(result.getApiKey());
        assertTrue(result.getApiKey().startsWith("mk_live_"));
        assertNotNull(result.getKeyInfo());
        assertEquals("Test API Key", result.getKeyInfo().getName());
        assertEquals(1, result.getKeyInfo().getPermissions().size());
    }

    @Test
    void testCreateApiKey_WithExpiration() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(30);

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Expiring Key")
                .expiresAt(futureDate)
                .rateLimitPerMinute(1000)
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.WRITE,
                                ApiResourceType.SCHEMA,
                                null
                        )
                ))
                .build();

        CreateApiKeyResult result = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                request
        );

        assertNotNull(result.getKeyInfo().getExpiresAt());
        assertTrue(result.getKeyInfo().getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Test
    void testCreateApiKey_PastExpiration_ThrowsException() {
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Invalid Key")
                .expiresAt(pastDate)
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        assertThrows(BadRequestException.class, () -> {
            apiKeyService.createApiKey(testUser.getId(), testOrg.getId(), request);
        });
    }

    @Test
    void testCreateApiKey_InvalidOrganization_ThrowsException() {
        UUID invalidOrgId = UUID.randomUUID();

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test Key")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> {
            apiKeyService.createApiKey(testUser.getId(), invalidOrgId, request);
        });
    }

    @Test
    void testListOrganizationKeys() {
        // Create 2 keys
        for (int i = 0; i < 2; i++) {
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

        List<ApiKeyResponse> keys = apiKeyService.listOrganizationKeys(
                testUser.getId(),
                testOrg.getId()
        );

        assertEquals(2, keys.size());
    }

    @Test
    void testGetApiKeyById() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test Key")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        CreateApiKeyResult result = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                request
        );

        ApiKeyResponse retrieved = apiKeyService.getApiKeyById(
                testUser.getId(),
                testOrg.getId(),
                result.getKeyInfo().getId()
        );

        assertNotNull(retrieved);
        assertEquals("Test Key", retrieved.getName());
    }

    @Test
    void testUpdateApiKey() {
        // Create key
        CreateApiKeyRequest createRequest = CreateApiKeyRequest.builder()
                .name("Original Name")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        CreateApiKeyResult result = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                createRequest
        );

        // Update key
        UpdateApiKeyRequest updateRequest = UpdateApiKeyRequest.builder()
                .name("Updated Name")
                .rateLimitPerMinute(2000)
                .build();

        ApiKeyResponse updated = apiKeyService.updateApiKey(
                testUser.getId(),
                testOrg.getId(),
                result.getKeyInfo().getId(),
                updateRequest
        );

        assertEquals("Updated Name", updated.getName());
        assertEquals(2000, updated.getRateLimitPerMinute());
    }

    @Test
    void testRevokeApiKey() {
        // Create key
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Key to Revoke")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        CreateApiKeyResult result = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                request
        );

        // Revoke key
        apiKeyService.revokeApiKey(testUser.getId(), testOrg.getId(), result.getKeyInfo().getId());

        // Verify revoked
        ApiKeyResponse revoked = apiKeyService.getApiKeyById(
                testUser.getId(),
                testOrg.getId(),
                result.getKeyInfo().getId()
        );

        assertFalse(revoked.isActive());
    }

    @Test
    void testDeleteApiKey() {
        // Create key
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Key to Delete")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        CreateApiKeyResult result = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                request
        );

        UUID keyId = result.getKeyInfo().getId();

        // Delete key
        apiKeyService.deleteApiKey(testUser.getId(), testOrg.getId(), keyId);

        // Verify deleted
        assertThrows(ResourceNotFoundException.class, () -> {
            apiKeyService.getApiKeyById(testUser.getId(), testOrg.getId(), keyId);
        });
    }

    @Test
    void testRotateApiKey() {
        // Create original key
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Original Key")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.WRITE,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        CreateApiKeyResult original = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                request
        );

        UUID originalId = original.getKeyInfo().getId();

        // Rotate key
        CreateApiKeyResult rotated = apiKeyService.rotateApiKey(
                testUser.getId(),
                testOrg.getId(),
                originalId
        );

        // Verify new key created
        assertNotNull(rotated.getApiKey());
        assertNotEquals(original.getApiKey(), rotated.getApiKey());

        // Verify old key is revoked
        ApiKeyResponse oldKey = apiKeyService.getApiKeyById(
                testUser.getId(),
                testOrg.getId(),
                originalId
        );
        assertFalse(oldKey.isActive());

        // Verify new key is active
        assertTrue(rotated.getKeyInfo().isActive());
    }

    @Test
    void testCreateApiKey_MultiplePermissions() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Multi-Permission Key")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        ),
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.WRITE,
                                ApiResourceType.SCHEMA,
                                null
                        ),
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.ADMIN,
                                ApiResourceType.PROJECT,
                                UUID.randomUUID()
                        )
                ))
                .build();

        CreateApiKeyResult result = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                request
        );

        assertEquals(3, result.getKeyInfo().getPermissions().size());
    }

    @Test
    void testCreateApiKey_KeyPrefixMasked() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test Key")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        CreateApiKeyResult result = apiKeyService.createApiKey(
                testUser.getId(),
                testOrg.getId(),
                request
        );

        // Raw key should be full
        assertTrue(result.getApiKey().length() >= 40);

        // Key prefix in response should be masked
        assertTrue(result.getKeyInfo().getKeyPrefix().endsWith("****"));
        assertFalse(result.getKeyInfo().getKeyPrefix().contains(
                result.getApiKey().substring(10) // Part of the actual key
        ));
    }
}