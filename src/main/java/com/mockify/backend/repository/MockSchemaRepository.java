package com.mockify.backend.repository;

import com.mockify.backend.model.MockSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MockSchemaRepository extends JpaRepository<MockSchema, Long> {

    // Find all schemas under a project
    List<MockSchema> findByProjectId(Long projectId);

    // Find schema by name under a project
    MockSchema findByNameAndProjectId(String name, Long projectId);

    // Delete schemas under a project
    void deleteByProjectId(Long projectId);

    // Count all schemas
    long count();
}