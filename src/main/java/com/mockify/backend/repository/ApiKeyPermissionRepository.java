package com.mockify.backend.repository;

import com.mockify.backend.model.ApiKeyPermission;
import com.mockify.backend.model.ApiKeyPermission.ApiPermission;
import com.mockify.backend.model.ApiKeyPermission.ApiResourceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ApiKeyPermissionRepository extends JpaRepository<ApiKeyPermission, UUID> {

    /**
     * Find all permissions for a specific API key
     */
    List<ApiKeyPermission> findByApiKeyId(UUID apiKeyId);

    /**
     * Check if specific permission exists
     * Used to prevent duplicate permission grants
     */
    boolean existsByApiKeyIdAndPermissionAndResourceTypeAndResourceId(
            UUID apiKeyId,
            ApiPermission permission,
            ApiResourceType resourceType,
            UUID resourceId
    );

    /**
     * Check if wildcard permission exists
     */
    boolean existsByApiKeyIdAndPermissionAndResourceTypeAndResourceIdIsNull(
            UUID apiKeyId,
            ApiPermission permission,
            ApiResourceType resourceType
    );

    /**
     * Find permissions for a specific resource type and ID
     */
    @Query("""
        SELECT akp FROM ApiKeyPermission akp
        WHERE akp.apiKey.id = :apiKeyId
          AND akp.resourceType = :resourceType
          AND (akp.resourceId = :resourceId OR akp.resourceId IS NULL)
    """)
    List<ApiKeyPermission> findByApiKeyIdAndResourceTypeAndResourceId(
            @Param("apiKeyId") UUID apiKeyId,
            @Param("resourceType") ApiResourceType resourceType,
            @Param("resourceId") UUID resourceId
    );

    /**
     * Delete all permissions for an API key
     */
    void deleteByApiKeyId(UUID apiKeyId);
}