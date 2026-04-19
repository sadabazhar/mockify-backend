package com.mockify.backend.repository;

import com.mockify.backend.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    /**
     * PRIMARY AUTHENTICATION QUERY
     * Find active, non-expired API key by hash
     * Uses composite index for optimal performance
     */
    @Query("""
        SELECT ak FROM ApiKey ak
        LEFT JOIN FETCH ak.permissions
        WHERE ak.organization.id = :organizationId
          AND ak.keyHash = :keyHash
          AND ak.isActive = true
          AND (ak.expiresAt IS NULL OR ak.expiresAt > :now)
    """)
    Optional<ApiKey> findValidKeyByHash(
            @Param("organizationId") UUID organizationId,
            @Param("keyHash") String keyHash,
            @Param("now") LocalDateTime now
    );

    /**
     * Alternative authentication: Find by key prefix and validate
     * Used when organization is not known upfront
     * Less efficient but necessary for API key authentication
     */
    @Query("""
        SELECT ak FROM ApiKey ak
        LEFT JOIN FETCH ak.permissions
        WHERE ak.keyPrefix = :keyPrefix
          AND ak.isActive = true
          AND (ak.expiresAt IS NULL OR ak.expiresAt > :now)
    """)
    List<ApiKey> findByKeyPrefixAndActive(
            @Param("keyPrefix") String keyPrefix,
            @Param("now") LocalDateTime now
    );

    /**
     * Find all API keys for an organization
     */
    @Query("""
        SELECT ak FROM ApiKey ak
        LEFT JOIN FETCH ak.permissions
        WHERE ak.organization.id = :organizationId
        ORDER BY ak.createdAt DESC
    """)
    List<ApiKey> findByOrganizationId(@Param("organizationId") UUID organizationId);

    /**
     * Find all API keys for a specific project
     */
    @Query("""
        SELECT ak FROM ApiKey ak
        LEFT JOIN FETCH ak.permissions
        WHERE ak.project.id = :projectId
        ORDER BY ak.createdAt DESC
    """)
    List<ApiKey> findByProjectId(@Param("projectId") UUID projectId);

    /**
     * Find API keys created by a specific user
     */
    @Query("""
        SELECT ak FROM ApiKey ak
        LEFT JOIN FETCH ak.permissions
        WHERE ak.createdBy.id = :userId
        ORDER BY ak.createdAt DESC
    """)
    List<ApiKey> findByCreatedById(@Param("userId") UUID userId);

    /**
     * Check if a key with this hash already exists in the organization
     * Used during key generation to prevent collisions
     */
    boolean existsByOrganizationIdAndKeyHash(UUID organizationId, String keyHash);

    /**
     * Find expired keys for cleanup
     */
    @Query("""
        SELECT ak FROM ApiKey ak
        WHERE ak.expiresAt IS NOT NULL
          AND ak.expiresAt < :now
    """)
    List<ApiKey> findExpiredKeys(@Param("now") LocalDateTime now);

    /**
     * Count active keys for an organization
     */
    @Query("""
        SELECT COUNT(ak) FROM ApiKey ak
        WHERE ak.organization.id = :organizationId
          AND ak.isActive = true
          AND (ak.expiresAt IS NULL OR ak.expiresAt > :now)
    """)
    long countActiveKeysByOrganization(
            @Param("organizationId") UUID organizationId,
            @Param("now") LocalDateTime now
    );
}