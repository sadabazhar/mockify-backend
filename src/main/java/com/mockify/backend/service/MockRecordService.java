package com.mockify.backend.service;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockRecordService {

    MockRecordResponse createRecord(UUID userId, UUID schemaId, CreateMockRecordRequest request);

    List<MockRecordResponse> createRecordsBulk(UUID userId, UUID schemaId, List<CreateMockRecordRequest> requests);

    MockRecordResponse getRecordById(UUID userId, UUID recordId);

    List<MockRecordResponse> getRecordsBySchemaId(UUID userId, UUID schemaId);

    MockRecordResponse updateRecord(UUID userId, UUID recordId, UpdateMockRecordRequest request);

    void deleteRecord(UUID userId, UUID recordId);

    long countRecords();

    List<MockRecordResponse> autoGenerateRecordsBulk(
            UUID userId,
            UUID schemaId,
            int count
    );

}
