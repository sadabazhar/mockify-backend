package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.service.MockSchemaService;
import org.springframework.stereotype.Service;

import java.util.List;

//Handles mock schema CRUD and structure definition for APIs.
@Service
public class MockSchemaServiceImpl implements MockSchemaService {

    // Create new schema under a project
    @Override
    public MockSchemaResponse createSchema(CreateMockSchemaRequest request) {
        return null;
    }

    // Get all schemas for a project
    @Override
    public List<MockSchemaResponse> getSchemasByProjectId(Long projectId) {
        return List.of();
    }

    // Get a schema by its ID
    @Override
    public MockSchemaResponse getSchemaById(Long schemaId) {
        return null;
    }

    // Update schema definition or name
    @Override
    public MockSchemaResponse updateSchema(Long schemaId, UpdateMockSchemaRequest request) {
        return null;
    }

    // Delete schema and its records
    @Override
    public void deleteSchema(Long schemaId) {

    }

    // Count all schemas
    @Override
    public long countSchemas() {
        return 0;
    }
}
