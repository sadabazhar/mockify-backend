package com.mockify.backend.repository;

import com.mockify.backend.model.MockSchema;

import jakarta.transaction.Transactional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MockSchemaRepository extends JpaRepository<MockSchema, Long> {
        // Find all schemas under a project
    @Query("SELECT m FROM MockSchema m WHERE m.project.id = :projectId")
    List<MockSchema> findByProjectId(@Param("projectId") Long projectId);

    // Find schema by name under a project
    @Query("SELECT m FROM MockSchema m WHERE m.name = :name AND m.project.id = :projectId")
    MockSchema findByNameAndProjectId(@Param("name") String name, @Param("projectId") Long projectId);

    // Delete schemas under a project
    @Modifying
    @Transactional
    @Query("DELETE FROM MockSchema m WHERE m.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    // Count all schemas
    @Query("SELECT COUNT(m) FROM MockSchema m")
    long countAllSchemas();
}