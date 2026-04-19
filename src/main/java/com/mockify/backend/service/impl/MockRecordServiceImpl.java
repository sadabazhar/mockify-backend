package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockRecordMapper;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.repository.MockRecordRepository;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.service.MockRecordService;
import com.mockify.backend.service.MockValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockRecordServiceImpl implements MockRecordService {

    private final MockRecordRepository mockRecordRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordMapper mockRecordMapper;
    private final MockValidatorService mockValidatorService;

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'RECORD:WRITE')")
    public MockRecordResponse createRecord(UUID userId, UUID schemaId, CreateMockRecordRequest request) {
        if (request == null)           throw new BadRequestException("Request cannot be null");
        if (schemaId == null)          throw new BadRequestException("Schema ID is required");
        if (request.getData() == null) throw new BadRequestException("Record data cannot be null");

        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        // VALIDATE DATA
        mockValidatorService.validateRecordAgainstSchema(schema.getSchemaJson(), request.getData());

        MockRecordResponse response = persistRecord(schema, request);
        log.info("Record created in schema {} by user {}", schemaId, userId);
        return response;
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'RECORD:WRITE')")
    public List<MockRecordResponse> createRecordsBulk(UUID userId, UUID schemaId, List<CreateMockRecordRequest> requests) {
        log.info("Bulk create requested by userId={} count={}", userId, requests == null ? 0 : requests.size());
        if (requests == null || requests.isEmpty())
            throw new BadRequestException("Records list cannot be null or empty");

        // @PreAuthorize already fired once for the whole batch.
        // IMPORTANT: we call persistRecord() directly, NOT createRecord().
        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        log.info("Bulk creating {} records in schema {} by user {}", requests.size(), schemaId, userId);

        return requests.stream()
                .map(req -> {
                    if (req == null || req.getData() == null)
                        throw new BadRequestException("Record data cannot be null");
                    mockValidatorService.validateRecordAgainstSchema(schema.getSchemaJson(), req.getData());
                    return persistRecord(schema, req);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#recordId, 'RECORD', 'READ')")
    public MockRecordResponse getRecordById(UUID userId, UUID recordId) {
        log.debug("Fetching record for userId={}, recordId={}", userId, recordId);

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'RECORD:READ')")
    public List<MockRecordResponse> getRecordsBySchemaId(UUID userId, UUID schemaId) {
        log.debug("Fetching records for userId={}, schemaId={}", userId, schemaId);

        List<MockRecord> records = mockRecordRepository.findByMockSchema_Id(schemaId);
        return mockRecordMapper.toResponseList(records);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#recordId, 'RECORD', 'WRITE')")
    public MockRecordResponse updateRecord(UUID userId, UUID recordId, UpdateMockRecordRequest request) {
        log.info("Updating record userId={}, recordId={}", userId, recordId);

        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        if (request.getData() != null) {
            mockValidatorService.validateRecordAgainstSchema(
                    record.getMockSchema().getSchemaJson(), request.getData());
        }

        mockRecordMapper.updateEntityFromRequest(request, record);
        mockRecordRepository.save(record);

        log.info("Record {} updated by user {}", recordId, userId);
        return mockRecordMapper.toResponse(record);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#recordId, 'RECORD', 'DELETE')")
    public void deleteRecord(UUID userId, UUID recordId) {
        log.warn("Deleting record userId={}, recordId={}", userId, recordId);

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        log.warn("Record {} deleted by user {}", recordId, userId);
        mockRecordRepository.delete(record);
    }

    @Transactional
    void deleteExpiredRecords() {
        log.info("Deleting expired records...");
        List<MockRecord> expired = mockRecordRepository.findByExpiresAtBefore(LocalDateTime.now());
        mockRecordRepository.deleteAll(expired);
        log.info("Expired records deleted count={}", expired.size());
    }

    @Override
    @Transactional(readOnly = true)
    public long countRecords() {
        return mockRecordRepository.count();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Persists a single record against an already-loaded and already-authorised
     * schema. private so it can be called by both {@link #createRecord}
     * and {@link #createRecordsBulk} only
     *
     * <p>Do NOT annotate this method with {@code @PreAuthorize} — callers are
     * responsible for ensuring authorisation has already been verified.</p>
     */
    private MockRecordResponse persistRecord(MockSchema schema, CreateMockRecordRequest request) {
        MockRecord record = mockRecordMapper.toEntity(request);
        record.setMockSchema(schema);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusDays(7));
        mockRecordRepository.save(record);
        return mockRecordMapper.toResponse(record);
    }
}