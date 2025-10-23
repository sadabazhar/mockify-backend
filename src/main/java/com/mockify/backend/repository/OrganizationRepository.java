package com.mockify.backend.repository;

import com.mockify.backend.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    // Find all organizations owned by a user
    List<Organization> findByOwnerId(Long ownerId);

    // Check if organization exists by name
    boolean existsByName(String name);

    // Delete by owner
    void deleteByOwnerId(Long ownerId);

    // Count all organizations
    long count();
}