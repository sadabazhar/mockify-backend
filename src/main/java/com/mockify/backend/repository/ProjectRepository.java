package com.mockify.backend.repository;

import com.mockify.backend.model.Project;

import jakarta.transaction.Transactional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;                                 

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // Find all projects under an organization
@Query("SELECT p FROM Project p WHERE p.organization.id = :organizationId")
List<Project> findByOrganizationId(@Param("organizationId") Long organizationId);


// Find project by name and organization
@Query("SELECT p FROM Project p WHERE p.name = :name AND p.organization.id = :organizationId")
Project findByNameAndOrganizationId(@Param("name") String name, @Param("organizationId") Long organizationId);


// Delete all projects under an organization
@Modifying
@Transactional
@Query("DELETE FROM Project p WHERE p.organization.id = :organizationId")
void deleteByOrganizationId(@Param("organizationId") Long organizationId);


// Count all projects
@Query("SELECT COUNT(p) FROM Project p")
long countAllProjects();

}