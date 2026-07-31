package com.mockify.backend.service;

import com.mockify.backend.dto.response.schema.MockSchemaVersionResponse;
import com.mockify.backend.dto.response.schema.MockSchemaVersionSummaryResponse;
import com.mockify.backend.dto.response.schema.SchemaDiffResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.model.MockSchema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MockSchemaVersionService {

    // Record a historical version snapshot of the current state
    void recordSnapshot(MockSchema schema, String commitMessage, UUID changedByUserId);

    // Get paginated version history list
    Page<MockSchemaVersionSummaryResponse> getSchemaHistory(UUID userId, UUID schemaId, Pageable pageable);

    // Retrieve full snapshot of a specific version
    MockSchemaVersionResponse getSchemaVersion(UUID userId, UUID schemaId, Integer version);

    // Compare two specific versions and generate structural differences
    SchemaDiffResponse compareVersions(UUID userId, UUID schemaId, Integer sourceVersion, Integer targetVersion);

    // Secure forward-only rollback of a schema to a target historic configuration
    MockSchemaResponse rollbackSchema(UUID userId, UUID schemaId, Integer targetVersion, String reason);

    // Rollback one step back to the immediately preceding version
    MockSchemaResponse rollbackToPreviousVersion(UUID userId, UUID schemaId, String reason);
}
