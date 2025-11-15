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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MockRecordServiceImpl implements MockRecordService {

    private final MockRecordRepository mockRecordRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordMapper mockRecordMapper;
    private final MockValidatorService mockValidatorService;

    // CREATE single record
    @Override
    @Transactional
    public MockRecordResponse createRecord(CreateMockRecordRequest request) {

        log.info("Creating new mock record for schemaId={}",
                request != null ? request.getSchemaId() : null);

        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }
        if (request.getSchemaId() == null) {
            throw new BadRequestException("Schema ID is required");
        }

        MockSchema schema = mockSchemaRepository.findById(request.getSchemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        log.debug("Schema found: schemaId={}, validating record data...", schema.getId());

        // VALIDATE DATA
        Map<String, Object> schemaJson = schema.getSchemaJson();
        mockValidatorService.validateRecordAgainstSchema(schemaJson, request.getData());

        MockRecord record = mockRecordMapper.toEntity(request);
        record.setMockSchema(schema);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusDays(7));

        mockRecordRepository.save(record);

        log.info("Record created successfully: recordId={}", record.getId());

        return mockRecordMapper.toResponse(record);
    }

    // CREATE multiple records (bulk)
    @Override
    @Transactional
    public List<MockRecordResponse> createRecordsBulk(List<CreateMockRecordRequest> requests) {
        log.info("Bulk create requested: {} records", requests.size());
        return requests.stream()
                .map(this::createRecord)
                .toList();
    }

    // GET record by ID
    @Override
    @Transactional(readOnly = true)
    public MockRecordResponse getRecordById(Long recordId) {

        log.debug("Fetching record by id={}", recordId);

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        return mockRecordMapper.toResponse(record);
    }

    // GET all records under a schema
    @Override
    @Transactional(readOnly = true)
    public List<MockRecordResponse> getRecordsBySchemaId(Long schemaId) {

        log.debug("Fetching all records for schemaId={}", schemaId);

        List<MockRecord> records = mockRecordRepository.findByMockSchema_Id(schemaId);
        return mockRecordMapper.toResponseList(records);
    }

    // UPDATE record
    @Override
    @Transactional
    public MockRecordResponse updateRecord(Long recordId, UpdateMockRecordRequest request) {

        log.info("Updating record recordId={}", recordId);

        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        MockSchema schema = record.getMockSchema();

        if (request.getData() != null) {
            log.debug("Validating update data for recordId={}", recordId);
            Map<String, Object> schemaJson = schema.getSchemaJson();
            mockValidatorService.validateRecordAgainstSchema(schemaJson, request.getData());
        }

        mockRecordMapper.updateEntityFromRequest(request, record);
        mockRecordRepository.save(record);

        log.info("Record updated successfully recordId={}", recordId);

        return mockRecordMapper.toResponse(record);
    }

    // DELETE record by ID
    @Override
    @Transactional
    public void deleteRecord(Long recordId) {

        log.warn("Deleting record recordId={}", recordId);

        if (!mockRecordRepository.existsById(recordId)) {
            throw new ResourceNotFoundException("Record not found");
        }

        mockRecordRepository.deleteById(recordId);

        log.info("Record deleted recordId={}", recordId);
    }

    // DELETE expired records
    @Override
    @Transactional
    public void deleteExpiredRecords() {
        log.info("Deleting expired records...");

        List<MockRecord> expired = mockRecordRepository.findByExpiresAtBefore(LocalDateTime.now());
        mockRecordRepository.deleteAll(expired);

        log.info("Expired records deleted count={}", expired.size());
    }

    // COUNT total records
    @Override
    @Transactional(readOnly = true)
    public long countRecords() {
        long count = mockRecordRepository.count();
        log.debug("Total mock records count={}", count);
        return count;
    }
}
