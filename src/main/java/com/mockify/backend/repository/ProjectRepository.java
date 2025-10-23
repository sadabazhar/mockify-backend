package com.mockify.backend.repository;

import com.mockify.backend.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Find all projects under an organization
    List<Project> findByOrganizationId(Long organizationId);

    // Find project by name and organization
    Project findByNameAndOrganizationId(String name, Long organizationId);

    // Delete all projects under an organization
    void deleteByOrganizationId(Long organizationId);

    // Count all projects
    long count();
}