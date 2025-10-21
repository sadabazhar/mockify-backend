package com.mockify.backend.repository;

import com.mockify.backend.model.MockSchema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MockSchemaRepository extends JpaRepository<MockSchema, Long> {
}