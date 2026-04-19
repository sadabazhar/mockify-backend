package com.mockify.backend.repository;

import com.mockify.backend.model.MockSchema;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MockSchemaRepository extends JpaRepository<MockSchema, UUID> {

    // Find all schemas under a project
    List<MockSchema> findByProjectId(UUID projectId);

    // Find schema by name under a project
    MockSchema findByNameAndProjectId(String name, UUID projectId);

    // Delete schemas under a project
    void deleteByProjectId(UUID projectId);

    // Count all schemas
    long count();

    Optional<MockSchema> findBySlugAndProjectId(String slug, UUID projectId);

    boolean existsBySlugAndProjectId(String slug, UUID projectId);

    // Eager-load full hierarchy for permission evaluation (avoids LazyInitializationException)
    @EntityGraph(attributePaths = {
            "project",
            "project.organization",
            "project.organization.owner"
    })
    Optional<MockSchema> findWithContextById(@Param("id") UUID id);
}