package com.mockify.backend.controller;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.service.MockRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MockRecordController {

    private final MockRecordService mockRecordService;

    // CREATE a new record
    @PostMapping("/records")
    public ResponseEntity<MockRecordResponse> createRecord(
            @RequestBody CreateMockRecordRequest request) {
        MockRecordResponse response = mockRecordService.createRecord(request);
        return ResponseEntity.ok(response);
    }

    // CREATE multiple records (bulk)
    @PostMapping("/records/bulk")
    public ResponseEntity<List<MockRecordResponse>> createRecordsBulk(
            @RequestBody List<CreateMockRecordRequest> requests) {
        List<MockRecordResponse> response = mockRecordService.createRecordsBulk(requests);
        return ResponseEntity.ok(response);
    }

    // GET record by ID
    @GetMapping("/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecordById(@PathVariable Long recordId) {
        MockRecordResponse response = mockRecordService.getRecordById(recordId);
        return ResponseEntity.ok(response);
    }

    // GET all records under a schema
    @GetMapping("/records/schema/{schemaId}")
    public ResponseEntity<List<MockRecordResponse>> getRecordsBySchema(@PathVariable Long schemaId) {
        List<MockRecordResponse> response = mockRecordService.getRecordsBySchemaId(schemaId);
        return ResponseEntity.ok(response);
    }

    // UPDATE record
    @PutMapping("/records/{recordId}")
    public ResponseEntity<MockRecordResponse> updateRecord(
            @PathVariable Long recordId,
            @RequestBody UpdateMockRecordRequest request) {
        MockRecordResponse response = mockRecordService.updateRecord(recordId, request);
        return ResponseEntity.ok(response);
    }

    // DELETE record by ID
    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long recordId) {
        mockRecordService.deleteRecord(recordId);
        return ResponseEntity.noContent().build();
    }

    // COUNT total records
    @GetMapping("/records/count")
    public ResponseEntity<Long> countRecords() {
        long count = mockRecordService.countRecords();
        return ResponseEntity.ok(count);
    }
}
