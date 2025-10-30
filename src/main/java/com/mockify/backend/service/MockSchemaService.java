package com.mockify.backend.service;

import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;

import java.util.List;
import java.util.Optional;

public interface MockSchemaService {

    //Create new schema under a project
    MockSchemaResponse createSchema(CreateMockSchemaRequest request);

    // Get all schemas for a project
    List<MockSchemaResponse> getSchemasByProjectId(Long projectId);

    // Get a schema by its ID
    MockSchemaResponse getSchemaById(Long schemaId);

    // Update schema definition or name
    MockSchemaResponse updateSchema(Long schemaId, UpdateMockSchemaRequest request);

    // Delete schema and its records
    void deleteSchema(Long schemaId);

    // Count all schemas
    long countSchemas();
}
