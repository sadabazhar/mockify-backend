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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MockRecordServiceImpl implements MockRecordService {

    private final MockRecordRepository mockRecordRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordMapper mockRecordMapper;

    // CREATE single record
    @Override
    @Transactional
    public MockRecordResponse createRecord(CreateMockRecordRequest request) {

        if (request == null) {
            throw new BadRequestException("Request cannot be null");
        }
        if (request.getSchemaId() == null) {
            throw new BadRequestException("Schema ID is required");
        }

        MockSchema schema = mockSchemaRepository.findById(request.getSchemaId())
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        MockRecord record = mockRecordMapper.toEntity(request);
        record.setMockSchema(schema);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusDays(7));

        mockRecordRepository.save(record);
        return mockRecordMapper.toResponse(record);
    }

    // CREATE multiple records (bulk)
    @Override
    @Transactional
    public List<MockRecordResponse> createRecordsBulk(List<CreateMockRecordRequest> requests) {
        return requests.stream()
                .map(this::createRecord)
                .toList();
    }

    // GET record by ID
    @Override
    @Transactional(readOnly = true)
    public MockRecordResponse getRecordById(Long recordId) {
        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));
        return mockRecordMapper.toResponse(record);
    }

    // GET all records under a schema
    @Override
    @Transactional(readOnly = true)
    public List<MockRecordResponse> getRecordsBySchemaId(Long schemaId) {
        List<MockRecord> records = mockRecordRepository.findByMockSchema_Id(schemaId);
        return mockRecordMapper.toResponseList(records);
    }

    // UPDATE record
    @Override
    @Transactional
    public MockRecordResponse updateRecord(Long recordId, UpdateMockRecordRequest request) {
        MockRecord record = mockRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Record not found"));

        mockRecordMapper.updateEntityFromRequest(request, record);

        mockRecordRepository.save(record);
        return mockRecordMapper.toResponse(record);
    }

    // DELETE record by ID
    @Override
    @Transactional
    public void deleteRecord(Long recordId) {
        if (!mockRecordRepository.existsById(recordId)) {
            throw new ResourceNotFoundException("Record not found");
        }
        mockRecordRepository.deleteById(recordId);
    }

    // DELETE expired records automatically
    @Override
    @Transactional
    public void deleteExpiredRecords() {
        List<MockRecord> expired = mockRecordRepository.findByExpiresAtBefore(LocalDateTime.now());
        mockRecordRepository.deleteAll(expired);
    }

    // COUNT total records
    @Override
    @Transactional(readOnly = true)
    public long countRecords() {
        return mockRecordRepository.count();
    }
}
