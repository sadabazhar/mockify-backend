package com.mockify.backend.service.impl;

//Handles mock data records tied to schemas.

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.service.MockRecordService;
import org.springframework.stereotype.Service;

import java.util.List;

// Handles mock data records tied to schemas.
@Service
public class MockRecordServiceImpl implements MockRecordService {

    // Create new record under schema
    @Override
    public MockRecordResponse createRecord(CreateMockRecordRequest request) {
        return null;
    }

    // Create new record under schema
    @Override
    public List<MockRecordResponse> createRecordsBulk(List<CreateMockRecordRequest> requests) {
        return List.of();
    }

    // Get record by ID
    @Override
    public MockRecordResponse getRecordById(Long recordId) {
        return null;
    }

    // Get all records under a schema
    @Override
    public List<MockRecordResponse> getRecordsBySchemaId(Long schemaId) {
        return List.of();
    }

    // Update record data
    @Override
    public MockRecordResponse updateRecord(Long recordId, UpdateMockRecordRequest request) {
        return null;
    }

    // Delete record by ID
    @Override
    public void deleteRecord(Long recordId) {

    }

    // Delete expired records periodically
    @Override
    public void deleteExpiredRecords() {

    }

    // Count total records
    @Override
    public long countRecords() {
        return 0;
    }
}
