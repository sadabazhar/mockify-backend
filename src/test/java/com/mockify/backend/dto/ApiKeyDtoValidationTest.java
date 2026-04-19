package com.mockify.backend.dto;

import com.mockify.backend.dto.request.apikey.CreateApiKeyRequest;
import com.mockify.backend.dto.request.apikey.UpdateApiKeyRequest;
import com.mockify.backend.model.ApiKeyPermission.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testCreateApiKeyRequest_Valid() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test Key")
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

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty(),
                "Valid request should have no violations");
    }

    @Test
    void testCreateApiKeyRequest_NameRequired() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("") // Empty name
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("name is required")));
    }

    @Test
    void testCreateApiKeyRequest_NameTooLong() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("a".repeat(256)) // Exceeds 255 chars
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("must not exceed 255")));
    }

    @Test
    void testCreateApiKeyRequest_DescriptionTooLong() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test")
                .description("a".repeat(1001)) // Exceeds 1000 chars
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("must not exceed 1000")));
    }

    @Test
    void testCreateApiKeyRequest_RateLimitTooLow() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test")
                .rateLimitPerMinute(0) // Below minimum
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("must be at least 1")));
    }

    @Test
    void testCreateApiKeyRequest_RateLimitTooHigh() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test")
                .rateLimitPerMinute(100001) // Above maximum
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("must not exceed 100000")));
    }

    @Test
    void testCreateApiKeyRequest_PermissionsRequired() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test")
                .permissions(List.of()) // Empty list
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
                .anyMatch(v -> v.getMessage().contains("At least one permission")));
    }

    @Test
    void testCreateApiKeyRequest_PermissionFieldsRequired() {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test")
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                null, // Missing permission
                                null, // Missing resource type
                                null
                        )
                ))
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.size() >= 2); // Both fields missing
    }

    @Test
    void testUpdateApiKeyRequest_Valid() {
        UpdateApiKeyRequest request = UpdateApiKeyRequest.builder()
                .name("Updated Name")
                .isActive(true)
                .rateLimitPerMinute(2000)
                .build();

        Set<ConstraintViolation<UpdateApiKeyRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void testUpdateApiKeyRequest_AllFieldsOptional() {
        UpdateApiKeyRequest request = new UpdateApiKeyRequest();

        Set<ConstraintViolation<UpdateApiKeyRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty(),
                "Update request should allow all fields to be null");
    }

    @Test
    void testCreateApiKeyRequest_WithProjectScope() {
        UUID projectId = UUID.randomUUID();

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Project Scoped Key")
                .projectId(projectId)
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.WRITE,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
        assertEquals(projectId, request.getProjectId());
    }

    @Test
    void testCreateApiKeyRequest_WithExpiration() {
        LocalDateTime futureDate = LocalDateTime.now().plusDays(30);

        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Expiring Key")
                .expiresAt(futureDate)
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        Set<ConstraintViolation<CreateApiKeyRequest>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
        assertEquals(futureDate, request.getExpiresAt());
    }

    @Test
    void testPermissionRequest_ScopedPermission() {
        UUID resourceId = UUID.randomUUID();

        CreateApiKeyRequest.PermissionRequest permission =
                new CreateApiKeyRequest.PermissionRequest(
                        ApiPermission.WRITE,
                        ApiResourceType.SCHEMA,
                        resourceId
                );

        assertEquals(ApiPermission.WRITE, permission.getPermission());
        assertEquals(ApiResourceType.SCHEMA, permission.getResourceType());
        assertEquals(resourceId, permission.getResourceId());
    }

    @Test
    void testPermissionRequest_WildcardPermission() {
        CreateApiKeyRequest.PermissionRequest permission =
                new CreateApiKeyRequest.PermissionRequest(
                        ApiPermission.ADMIN,
                        ApiResourceType.ORGANIZATION,
                        null // Wildcard
                );

        assertEquals(ApiPermission.ADMIN, permission.getPermission());
        assertEquals(ApiResourceType.ORGANIZATION, permission.getResourceType());
        assertNull(permission.getResourceId());
    }
}