package com.mockify.backend.controller;

import com.mockify.backend.dto.request.schema.RollbackRequest;
import com.mockify.backend.dto.response.page.PageResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.dto.response.schema.MockSchemaVersionResponse;
import com.mockify.backend.dto.response.schema.MockSchemaVersionSummaryResponse;
import com.mockify.backend.dto.response.schema.SchemaDiffResponse;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MockSchemaVersionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Mock Schema Versioning")
public class SchemaVersionController {

    private final MockSchemaVersionService schemaVersionService;
    private final EndpointService endpointService;

    /**
     * Get paginated version history list of a specific schema.
     */
    @GetMapping("/{org}/{project}/{schema}/versions")
    public ResponseEntity<PageResponse<MockSchemaVersionSummaryResponse>> getSchemaHistory(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PageableDefault(size = 20, sort = "version", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.debug("User {} fetching history page for schema {} (org={}, project={})", userId, schemaId, org, project);

        Page<MockSchemaVersionSummaryResponse> page = schemaVersionService.getSchemaHistory(userId, schemaId, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    /**
     * Retrieve full details of a specific schema version snapshot.
     */
    @GetMapping("/{org}/{project}/{schema}/versions/{versionNumber}")
    public ResponseEntity<MockSchemaVersionResponse> getSchemaVersion(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PathVariable Integer versionNumber,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.debug("User {} fetching version {} for schema {}", userId, versionNumber, schemaId);

        MockSchemaVersionResponse response = schemaVersionService.getSchemaVersion(userId, schemaId, versionNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Compare two specific versions structurally to inspect field difference logs.
     */
    @GetMapping("/{org}/{project}/{schema}/compare")
    public ResponseEntity<SchemaDiffResponse> compareVersions(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @RequestParam Integer sourceVersion,
            @RequestParam Integer targetVersion,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        log.debug("User {} running diff compare on schema {} versions {} vs {}", userId, schemaId, sourceVersion, targetVersion);

        SchemaDiffResponse diff = schemaVersionService.compareVersions(userId, schemaId, sourceVersion, targetVersion);
        return ResponseEntity.ok(diff);
    }

    /**
     * Rollback one step back to the immediately preceding version.
     * No request body needed — just POST to /rollback.
     * Optionally pass { "rollbackReason": "..." } for audit trail.
     */
    @PostMapping("/{org}/{project}/{schema}/rollback")
    public ResponseEntity<MockSchemaResponse> rollbackToPrevious(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @RequestBody(required = false) RollbackRequest request,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        String reason = (request != null) ? request.getRollbackReason() : null;

        log.warn("User {} rolling back schema {} one step (org={}, project={})", userId, schemaId, org, project);

        MockSchemaResponse response = schemaVersionService.rollbackToPreviousVersion(userId, schemaId, reason);
        return ResponseEntity.ok(response);
    }

    /**
     * Rollback to a specific target version.
     * Body: { "targetVersion": N, "rollbackReason": "..." }
     */
    @PostMapping("/{org}/{project}/{schema}/rollback/{targetVersion}")
    public ResponseEntity<MockSchemaResponse> rollbackToVersion(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String schema,
            @PathVariable Integer targetVersion,
            @RequestBody(required = false) RollbackRequest request,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID schemaId = endpointService.resolveSchema(org, project, schema);
        String reason = (request != null) ? request.getRollbackReason() : null;

        log.warn("User {} rolling back schema {} to version {} (org={}, project={})", userId, schemaId, targetVersion, org, project);

        MockSchemaResponse response = schemaVersionService.rollbackSchema(userId, schemaId, targetVersion, reason);
        return ResponseEntity.ok(response);
    }
}
