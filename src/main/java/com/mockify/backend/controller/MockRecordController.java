package com.mockify.backend.controller;

import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MockRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Mock Record management controller.
 * <h3>AUTHORIZATION</h3>
   * <p>All methods are open to both JWT sessions and API key callers. Authorization
   * is enforced declaratively via {@code @PreAuthorize("hasPermission(...)")}
   * annotations on each {@link MockRecordService} method, evaluated by
   * {@link com.mockify.backend.security.MockifyPermissionEvaluator}.</p>
   *
   * <ul>
   *   <li><b>JWT callers</b>: full access to all resources within their owned organization.</li>
   *   <li><b>API key callers</b>: access gated by three sequential guards —
   *       org scope → project scope → permission level
   *       (hierarchy: ADMIN ⊇ DELETE ⊇ WRITE ⊇ READ).</li>
   * </ul>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Mock Record")
public class MockRecordController {

    private final MockRecordService mockRecordService;
    private final EndpointService endpointService;

    // Create a new mock record
    @PostMapping("/{org}/{project}/{schema}/records")
    public ResponseEntity<MockRecordResponse> createRecord(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @Valid @RequestBody CreateMockRecordRequest request,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.info("User {} creating new mock record under schema {}", userId, schemaId);

        MockRecordResponse created = mockRecordService.createRecord(userId, schemaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Create multiple records in bulk
    @PostMapping("/{org}/{project}/{schema}/records/bulk")
    public ResponseEntity<List<MockRecordResponse>> createRecordsBulk(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @Valid @RequestBody List<CreateMockRecordRequest> requests,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.info("User {} bulk creating {} records under schema {}", userId, requests.size(), schemaId);

        List<MockRecordResponse> created = mockRecordService.createRecordsBulk(userId, schemaId, requests);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Get a record by ID
    @GetMapping("/{org}/{project}/{schema}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> getRecordById(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PathVariable UUID recordId,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        log.debug("User {} fetching record with ID {}", userId, recordId);

        MockRecordResponse record = mockRecordService.getRecordById(userId, recordId);
        return ResponseEntity.ok(record);
    }

    // Get all records under a specific schema
    @GetMapping("/{org}/{project}/{schema}/records")
    public ResponseEntity<List<MockRecordResponse>> getRecords(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.debug("User {} fetching all records under schema {}", userId, schemaId);

        List<MockRecordResponse> records = mockRecordService.getRecordsBySchemaId(userId, schemaId);
        return ResponseEntity.ok(records);
    }

    // Update an existing mock record
    @PutMapping("/{org}/{project}/{schema}/records/{recordId}")
    public ResponseEntity<MockRecordResponse> updateRecord(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateMockRecordRequest request,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        log.info("User {} updating record ID {}", userId, recordId);

        MockRecordResponse updated = mockRecordService.updateRecord(userId, recordId, request);
        return ResponseEntity.ok(updated);
    }

    // Delete a record by ID
    @DeleteMapping("/{org}/{project}/{schema}/records/{recordId}")
    public ResponseEntity<Void> deleteRecord(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PathVariable UUID recordId,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        log.warn("User {} deleting record ID {}", userId, recordId);

        mockRecordService.deleteRecord(userId, recordId);
        return ResponseEntity.noContent().build();
    }
}