package com.mockify.backend.service;

import com.mockify.backend.dto.request.apikey.CreateApiKeyRequest;
import com.mockify.backend.dto.response.apikey.CreateApiKeyResult;
import com.mockify.backend.model.*;
import com.mockify.backend.model.ApiKeyPermission.*;
import com.mockify.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiKeyServiceCollisionTest {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setName("Test User");
        testUser.setEmail("collision-test@example.com");
        testUser.setPassword("hashed_password");
        testUser.setProviderName("local");
        testUser.setEmailVerified(true);
        testUser = userRepository.save(testUser);

        testOrg = new Organization();
        testOrg.setName("Collision Test Org");
        testOrg.setSlug("collision-test-org");
        testOrg.setOwner(testUser);
        testOrg = organizationRepository.save(testOrg);
    }

    @Test
    void testGenerateMultipleKeys_NoCollisions() {
        Set<String> generatedKeys = new HashSet<>();
        int numberOfKeys = 100;

        for (int i = 0; i < numberOfKeys; i++) {
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

            CreateApiKeyResult result = apiKeyService.createApiKey(
                    testUser.getId(),
                    testOrg.getId(),
                    request
            );

            String apiKey = result.getApiKey();

            // Verify no collision
            assertFalse(generatedKeys.contains(apiKey),
                    "Collision detected on key " + i);

            generatedKeys.add(apiKey);
        }

        assertEquals(numberOfKeys, generatedKeys.size());
        System.out.println("Successfully generated " + numberOfKeys +
                " unique API keys with no collisions");
    }
}