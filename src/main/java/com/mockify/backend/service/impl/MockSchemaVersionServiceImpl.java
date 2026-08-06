package com.mockify.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.diff.AdvancedDiffEngine;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.dto.response.schema.MockSchemaVersionResponse;
import com.mockify.backend.dto.response.schema.MockSchemaVersionSummaryResponse;
import com.mockify.backend.dto.response.schema.SchemaDiffResponse;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockSchemaMapper;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.MockSchemaVersion;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.repository.MockSchemaVersionRepository;
import com.mockify.backend.service.MockSchemaVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockSchemaVersionServiceImpl implements MockSchemaVersionService {

    private final MockSchemaRepository mockSchemaRepository;
    private final MockSchemaVersionRepository mockSchemaVersionRepository;
    private final AdvancedDiffEngine diffEngine;
    private final MockSchemaMapper mockSchemaMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordSnapshot(MockSchema schema, String commitMessage, UUID changedByUserId) {
        log.debug("Recording snapshot for schema {} (version {})", schema.getId(), schema.getActiveVersion());

        // Deep-copy the current schema JSON map for the immutable snapshot
        Map<String, Object> snapshotMap = deepCopyMap(schema.getSchemaJson());

        MockSchemaVersion versionRecord = MockSchemaVersion.builder()
                .mockSchema(schema)
                .version(schema.getActiveVersion())
                .schemaJsonSnapshot(snapshotMap)
                .commitMessage(commitMessage != null ? commitMessage : "Updated schema JSON definition")
                .changedByUserId(changedByUserId)
                .build();

        // Add through the parent's collection — cascade=ALL handles the persist.
        // Saving via the version repository directly bypasses orphanRemoval tracking.
        schema.getVersions().add(versionRecord);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'READ')")
    public Page<MockSchemaVersionSummaryResponse> getSchemaHistory(UUID userId, UUID schemaId, Pageable pageable) {
        log.debug("User {} fetching version history for schema {}", userId, schemaId);

        if (!mockSchemaRepository.existsById(schemaId)) {
            throw new ResourceNotFoundException("Schema not found");
        }

        Page<MockSchemaVersion> versions = mockSchemaVersionRepository.findByMockSchemaId(schemaId, pageable);
        return versions.map(v -> MockSchemaVersionSummaryResponse.builder()
                .version(v.getVersion())
                .commitMessage(v.getCommitMessage())
                .changedByUserId(v.getChangedByUserId())
                .createdAt(v.getCreatedAt())
                .build());
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'READ')")
    public MockSchemaVersionResponse getSchemaVersion(UUID userId, UUID schemaId, Integer version) {
        log.debug("User {} fetching version {} for schema {}", userId, version, schemaId);

        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        MockSchemaVersion v = mockSchemaVersionRepository
                .findByMockSchemaIdAndVersion(schemaId, version)
                .orElseGet(() -> {
                    // Lazy-create version 1 for schemas that existed before versioning was added.
                    // Use the current schemaJson as the best available reconstruction.
                    if (version == 1) {
                        log.info("Version 1 missing for schema {} — creating on first access", schemaId);
                        Map<String, Object> snapshot = deepCopyMap(schema.getSchemaJson());
                        MockSchemaVersion v1 = MockSchemaVersion.builder()
                                .mockSchema(schema)
                                .version(1)
                                .schemaJsonSnapshot(snapshot)
                                .diffJson(null)
                                .commitMessage("Initial schema creation")
                                .changedByUserId(userId)
                                .build();
                        return mockSchemaVersionRepository.save(v1);
                    }
                    throw new ResourceNotFoundException("Schema version " + version + " not found");
                });

        return MockSchemaVersionResponse.builder()
                .id(v.getId())
                .schemaId(v.getMockSchema().getId())
                .version(v.getVersion())
                .schemaJsonSnapshot(v.getSchemaJsonSnapshot())
                .diffJson(v.getDiffJson())
                .commitMessage(v.getCommitMessage())
                .changedByUserId(v.getChangedByUserId())
                .createdAt(v.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'READ')")
    public SchemaDiffResponse compareVersions(UUID userId, UUID schemaId, Integer sourceVersion, Integer targetVersion) {
        log.debug("User {} comparing schema {} version {} with version {}", userId, schemaId, sourceVersion, targetVersion);

        if (!mockSchemaRepository.existsById(schemaId)) {
            throw new ResourceNotFoundException("Schema not found");
        }

        MockSchemaVersion source = mockSchemaVersionRepository.findByMockSchemaIdAndVersion(schemaId, sourceVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Source version " + sourceVersion + " not found"));

        MockSchemaVersion target = mockSchemaVersionRepository.findByMockSchemaIdAndVersion(schemaId, targetVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Target version " + targetVersion + " not found"));

        JsonNode patchNode = diffEngine.calculateRfc6902Patch(source.getSchemaJsonSnapshot(), target.getSchemaJsonSnapshot());

        return SchemaDiffResponse.builder()
                .schemaId(schemaId)
                .sourceVersion(sourceVersion)
                .targetVersion(targetVersion)
                .hasChanges(!patchNode.isEmpty())
                .differences(patchNode)
                .build();
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'WRITE')")
    public MockSchemaResponse rollbackSchema(UUID userId, UUID schemaId, Integer targetVersion, String reason) {
        log.warn("User {} rolling back schema {} to version {}", userId, schemaId, targetVersion);

        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        MockSchemaVersion targetSnapshot = mockSchemaVersionRepository.findByMockSchemaIdAndVersion(schemaId, targetVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Target version " + targetVersion + " not found"));

        Map<String, Object> oldPayload = targetSnapshot.getSchemaJsonSnapshot();
        Map<String, Object> currentPayload = schema.getSchemaJson();

        if (Objects.equals(currentPayload, oldPayload)) {
            throw new IllegalStateException("Active schema configuration is already identical to version " + targetVersion);
        }

        // Calculate forward patch for full auditing trail
        JsonNode patchNode = diffEngine.calculateRfc6902Patch(currentPayload, oldPayload);

        // Convert the patch node to a typed List for storage in the JSONB column (RFC 6902 patches are arrays)
        List<Map<String, Object>> diffData = objectMapper.convertValue(patchNode, new TypeReference<List<Map<String, Object>>>() {});

        // Save new version snapshot record representing the rollback action
        Integer nextVer = schema.getActiveVersion() + 1;

        // Deep-copy the rollback snapshot to prevent shared references
        Map<String, Object> rollbackSnapshot = deepCopyMap(oldPayload);

        MockSchemaVersion forwardVersion = MockSchemaVersion.builder()
                .mockSchema(schema)
                .version(nextVer)
                .schemaJsonSnapshot(rollbackSnapshot)
                .diffJson(diffData)
                .commitMessage(String.format("Rollback to version %d. Reason: %s", targetVersion,
                        (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "No reason provided"))
                .changedByUserId(userId)
                .build();

        // Add through the parent's collection — cascade=ALL handles the persist.
        schema.getVersions().add(forwardVersion);

        // Apply state revert forwardly
        schema.setSchemaJson(deepCopyMap(oldPayload));
        schema.setActiveVersion(nextVer);

        MockSchema saved = mockSchemaRepository.save(schema);
        return mockSchemaMapper.toResponse(saved);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'WRITE')")
    public MockSchemaResponse rollbackToPreviousVersion(UUID userId, UUID schemaId, String reason) {
        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        Integer currentVersion = schema.getActiveVersion();

        if (currentVersion <= 1) {
            throw new IllegalStateException("Schema is already at version 1 — nothing to roll back to");
        }

        Integer previousVersion = currentVersion - 1;
        log.warn("User {} undoing schema {} — reverting version {} → {}", userId, schemaId, currentVersion, previousVersion);

        // Load the previous version's snapshot to restore
        MockSchemaVersion previousSnapshot = mockSchemaVersionRepository
                .findByMockSchemaIdAndVersion(schemaId, previousVersion)
                .orElseThrow(() -> new ResourceNotFoundException("Previous version " + previousVersion + " not found"));

        // Delete the current (latest) version record — true undo, not forward-only
        mockSchemaVersionRepository.findByMockSchemaIdAndVersion(schemaId, currentVersion)
                .ifPresent(mockSchemaVersionRepository::delete);

        // Restore schema JSON and version pointer to the previous state
        schema.setSchemaJson(deepCopyMap(previousSnapshot.getSchemaJsonSnapshot()));
        schema.setActiveVersion(previousVersion);

        MockSchema saved = mockSchemaRepository.save(schema);
        return mockSchemaMapper.toResponse(saved);
    }

    /**
     * Creates an independent deep copy of a JSON map to ensure snapshot immutability.
     * Roundtrips through Jackson to handle arbitrarily nested structures.
     */
    private Map<String, Object> deepCopyMap(Map<String, Object> source) {
        if (source == null) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(source, new TypeReference<Map<String, Object>>() {});
    }
}
