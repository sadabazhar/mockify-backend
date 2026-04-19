package com.mockify.backend.service;

import com.mockify.backend.dto.request.apikey.CreateApiKeyRequest;
import com.mockify.backend.dto.request.apikey.UpdateApiKeyRequest;
import com.mockify.backend.dto.response.apikey.ApiKeyResponse;
import com.mockify.backend.dto.response.apikey.CreateApiKeyResult;

import java.util.List;
import java.util.UUID;

public interface ApiKeyService {

    /**
     * Create new API key
     * Returns the raw key which is shown ONCE
     */
    CreateApiKeyResult createApiKey(UUID userId, UUID organizationId, CreateApiKeyRequest request);

    /**
     * List all API keys for an organization
     */
    List<ApiKeyResponse> listOrganizationKeys(UUID userId, UUID organizationId);

    /**
     * List all API keys for a specific project
     */
    List<ApiKeyResponse> listProjectKeys(UUID userId, UUID projectId);

    /**
     * Get API key details by ID
     */
    ApiKeyResponse getApiKeyById(UUID userId, UUID organizationId, UUID keyId);

    /**
     * Update API key metadata (name, description, status, rate limit)
     * Cannot change permissions - must delete and recreate
     */
    ApiKeyResponse updateApiKey(UUID userId, UUID organizationId, UUID keyId, UpdateApiKeyRequest request);

    /**
     * Revoke (deactivate) API key
     */
    void revokeApiKey(UUID userId, UUID organizationId, UUID keyId);

    /**
     * Permanently delete API key
     */
    void deleteApiKey(UUID userId, UUID organizationId, UUID keyId);

    /**
     * Rotate API key (generate new key, revoke old one)
     */
    CreateApiKeyResult rotateApiKey(UUID userId, UUID organizationId, UUID keyId);
}