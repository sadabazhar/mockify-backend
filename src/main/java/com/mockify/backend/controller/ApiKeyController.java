package com.mockify.backend.controller;

import com.mockify.backend.dto.request.apikey.CreateApiKeyRequest;
import com.mockify.backend.dto.request.apikey.UpdateApiKeyRequest;
import com.mockify.backend.dto.response.apikey.ApiKeyResponse;
import com.mockify.backend.dto.response.apikey.CreateApiKeyResult;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.ApiKeyService;
import com.mockify.backend.service.EndpointService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API Key management controller.
 *
 * SECURITY: Every endpoint here requires JWT authentication.
 * API keys MUST NOT be usable to manage other API keys —
 * a leaked key must never be able to mint, rotate, or revoke credentials.
 *
 * Enforcement: {@link SecurityUtils#requireJwtAuthentication} is called first
 * in every method, throwing 403 if the caller is authenticated via an API key.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "API Keys", description = "API key management for programmatic access")
@Slf4j
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final EndpointService endpointService;

    @PostMapping("/{org}/api-keys")
    @Operation(summary = "Create new API key",
            description = "Generate a new API key with specified permissions. " +
                    "The raw key is returned ONCE and cannot be retrieved again. " +
                    "Requires JWT authentication.")
    public ResponseEntity<CreateApiKeyResult> createApiKey(
            @PathVariable String org,
            @Valid @RequestBody CreateApiKeyRequest request,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID organizationId = endpointService.resolveOrganization(org);

        log.info("User {} creating API key for organization {}", userId, organizationId);

        CreateApiKeyResult result = apiKeyService.createApiKey(userId, organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{org}/api-keys")
    @Operation(summary = "List organization API keys",
            description = "Requires JWT authentication.")
    public ResponseEntity<List<ApiKeyResponse>> listOrganizationKeys(
            @PathVariable String org,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID organizationId = endpointService.resolveOrganization(org);

        List<ApiKeyResponse> keys = apiKeyService.listOrganizationKeys(userId, organizationId);
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/{org}/{project}/api-keys")
    @Operation(summary = "List project API keys",
            description = "Requires JWT authentication.")
    public ResponseEntity<List<ApiKeyResponse>> listProjectKeys(
            @PathVariable String org,
            @PathVariable String project,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID projectId = endpointService.resolveProject(org, project);

        List<ApiKeyResponse> keys = apiKeyService.listProjectKeys(userId, projectId);
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/{org}/api-keys/{keyId}")
    @Operation(summary = "Get API key details",
            description = "Requires JWT authentication.")
    public ResponseEntity<ApiKeyResponse> getApiKey(
            @PathVariable String org,
            @PathVariable UUID keyId,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID organizationId = endpointService.resolveOrganization(org);
        ApiKeyResponse key = apiKeyService.getApiKeyById(userId, organizationId, keyId);
        return ResponseEntity.ok(key);
    }

    @PutMapping("/{org}/api-keys/{keyId}")
    @Operation(summary = "Update API key metadata",
            description = "Requires JWT authentication.")
    public ResponseEntity<ApiKeyResponse> updateApiKey(
            @PathVariable String org,
            @PathVariable UUID keyId,
            @Valid @RequestBody UpdateApiKeyRequest request,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID organizationId = endpointService.resolveOrganization(org);
        log.info("User {} updating API key {}", userId, keyId);

        ApiKeyResponse updated = apiKeyService.updateApiKey(userId, organizationId, keyId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{org}/api-keys/{keyId}/revoke")
    @Operation(summary = "Revoke API key",
            description = "Requires JWT authentication.")
    public ResponseEntity<Void> revokeApiKey(
            @PathVariable String org,
            @PathVariable UUID keyId,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID organizationId = endpointService.resolveOrganization(org);
        log.warn("User {} revoking API key {}", userId, keyId);

        apiKeyService.revokeApiKey(userId, organizationId, keyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{org}/api-keys/{keyId}")
    @Operation(summary = "Delete API key permanently",
            description = "Requires JWT authentication.")
    public ResponseEntity<Void> deleteApiKey(
            @PathVariable String org,
            @PathVariable UUID keyId,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID organizationId = endpointService.resolveOrganization(org);
        log.warn("User {} deleting API key {}", userId, keyId);

        apiKeyService.deleteApiKey(userId, organizationId, keyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{org}/api-keys/{keyId}/rotate")
    @Operation(summary = "Rotate API key",
            description = "Requires JWT authentication.")
    public ResponseEntity<CreateApiKeyResult> rotateApiKey(
            @PathVariable String org,
            @PathVariable UUID keyId,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID organizationId = endpointService.resolveOrganization(org);
        log.info("User {} rotating API key {}", userId, keyId);

        CreateApiKeyResult result = apiKeyService.rotateApiKey(userId, organizationId, keyId);
        return ResponseEntity.ok(result);
    }
}