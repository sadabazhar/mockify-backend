package com.mockify.backend.service;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;

import java.util.List;
import java.util.Optional;

public interface MockRecordService {

    // Create new record under schema
    MockRecordResponse createRecord(CreateMockRecordRequest request);

    // Create multiple records in one call (bulk)
    List<MockRecordResponse> createRecordsBulk(List<CreateMockRecordRequest> requests);

    // Get record by ID
    MockRecordResponse getRecordById(Long recordId);

    // Get all records under a schema
    List<MockRecordResponse> getRecordsBySchemaId(Long schemaId);

    // Update record data
    MockRecordResponse updateRecord(Long recordId, UpdateMockRecordRequest request);

    // Delete record by ID
    void deleteRecord(Long recordId);

    // Delete expired records periodically
    void deleteExpiredRecords();

    // Count total records
    long countRecords();
}
