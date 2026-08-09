package com.mockify.backend.repository;

import com.mockify.backend.dto.response.admin.AdminOrganizationResponse;
import com.mockify.backend.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    // Find all organizations owned by a user
    List<Organization> findByOwnerId(UUID ownerId);

    Page<Organization> findByOwnerId(UUID ownerId, Pageable pageable);

    // Find organization with owner and projects
    @Query("SELECT DISTINCT o FROM Organization o " +
            "JOIN FETCH o.owner " +
            "LEFT JOIN FETCH o.projects " +
            "WHERE o.id = :id")
    Optional<Organization> findByIdWithOwnerAndProjects(@Param("id") UUID id);

    // Check if organization exists by name
    boolean existsByName(String name);

    // Delete by owner
    void deleteByOwnerId(UUID ownerId);

    // Count all organizations
    long count();

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Used by the cleanup scheduler to find expired sandbox orgs.
     * Backed by idx_organizations_sandbox_expiry partial index.
     */
    List<Organization> findByIsSandboxTrueAndExpiresAtBefore(LocalDateTime now);

    /**
     * Used to return only real (non-sandbox) orgs in the user-facing listing.
     */
    Page<Organization> findByOwnerIdAndIsSandboxFalse(UUID ownerId, Pageable pageable);

    Optional<Organization> findByOwnerIdAndIsSandboxTrue(UUID ownerId);
}