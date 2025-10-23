package com.mockify.backend.repository;

import com.mockify.backend.model.Organization;

import jakarta.transaction.Transactional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    // Find all organizations owned by a user
@Query("SELECT o.id FROM Organization o WHERE o.owner.id = :ownerId")
List<Organization> findByOwnerId(@Param("id")Long ownerId);

// Check if organization exists by name
@Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END FROM Organization o WHERE o.name = :name")
boolean existsByName(@Param("name") String name);

// Delete by owner
@Modifying
@Transactional
@Query("DELETE FROM Organization o WHERE o.owner.id = :ownerId")
void deleteByOwnerId(@Param("ownerId") Long ownerId);

@Query("SELECT COUNT(o) FROM Organization o")
long countAllOrganizations();

}