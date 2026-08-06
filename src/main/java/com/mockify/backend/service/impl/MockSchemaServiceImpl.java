package com.mockify.backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.common.validation.PageableValidator;
import com.mockify.backend.diff.AdvancedDiffEngine;
import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaDetailResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockSchemaMapper;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.MockSchemaVersion;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.repository.MockSchemaVersionRepository;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MockSchemaService;
import com.mockify.backend.service.MockValidatorService;
import com.mockify.backend.service.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockSchemaServiceImpl implements MockSchemaService {

    private final MockSchemaRepository mockSchemaRepository;
    private final ProjectRepository projectRepository;
    private final MockSchemaMapper mockSchemaMapper;
    private final ObjectMapper objectMapper;
    private final MockValidatorService mockValidatorService;
    private final SlugService slugService;
    private final EndpointService endpointService;
    private final MockSchemaVersionRepository mockSchemaVersionRepository;
    private final AdvancedDiffEngine diffEngine;

    // Create a new mock schema under a specific project Only
    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'SCHEMA:WRITE')")
    public MockSchemaResponse createSchema(UUID userId, UUID projectId, CreateMockSchemaRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        if (mockSchemaRepository.findByNameAndProjectId(request.getName(), project.getId()) != null) {
            throw new DuplicateResourceException("Schema with the same name already exists in this project");
        }

        // Generate slug from name
        String slug = slugService.generateSlug(request.getName());

        // Check uniqueness within project
        if (mockSchemaRepository.existsBySlugAndProjectId(slug, projectId)) {
            slug = slugService.generateUniqueSlug(slug);
        }

        // Validate Mock Schema
        mockValidatorService.validateSchemaDefinition(request.getSchemaJson());

        MockSchema schema = mockSchemaMapper.toEntity(request);
        schema.setProject(project);
        schema.setSlug(slug);

        // Create initial version 1 snapshot
        Map<String, Object> snapshotMap = objectMapper.convertValue(schema.getSchemaJson(), new TypeReference<Map<String, Object>>() {});
        attachVersion(schema, 1, snapshotMap, null, "Initial schema creation", userId);

        MockSchema saved = mockSchemaRepository.save(schema);
        endpointService.createEndpoint(saved);

        log.info("Schema '{}' created in project {} by user {}", saved.getName(), projectId, userId);
        return mockSchemaMapper.toResponse(saved);
    }

    // Fetch all schemas under a project
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'SCHEMA:READ')")
    public Page<MockSchemaResponse> getSchemasByProjectId(UUID userId, UUID projectId, Pageable pageable) {

        // Validate Page size, protect from abuse
        PageableValidator.validate(pageable);

        Page<MockSchema> schemasPage =
                mockSchemaRepository.findByProjectId(projectId, pageable);

        log.debug("User {} fetching schemas page={}, size={} under project {}",
                userId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                projectId);

        return schemasPage.map(mockSchemaMapper::toResponse);
    }

    /*
     Fetch a specific schema
    */
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'READ')")
    public MockSchemaDetailResponse getSchemaById(UUID userId, UUID schemaId) {
        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));
        return mockSchemaMapper.toDetailResponse(schema);
    }

    /*
     Update schema (name or schema JSON)
     Ensures unique name and valid ownership
    */
    @Override
    @Transactional
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'WRITE')")
    public MockSchemaResponse updateSchema(UUID userId, UUID schemaId, UpdateMockSchemaRequest request) {
        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));

        // Check if new name conflicts with another schema in same project
        if (request.getName() != null && !request.getName().equals(schema.getName())) {
            MockSchema existing = mockSchemaRepository.findByNameAndProjectId(
                    request.getName(), schema.getProject().getId());
            if (existing != null && !existing.getId().equals(schema.getId())) {
                throw new DuplicateResourceException("Schema with this name already exists");
            }
        }

        // Validate Mock Schema
        if (request.getSchemaJson() != null) {
            mockValidatorService.validateSchemaDefinition(request.getSchemaJson());
        }

        // Determine if json structure has modified
        boolean isJsonChanged = request.getSchemaJson() != null && !request.getSchemaJson().equals(schema.getSchemaJson());

        // Deep-copy the current JSON before the mapper mutates the map in-place
        // (MapStruct's generated code calls .clear() + .putAll() on the existing map)
        Map<String, Object> previousJson = isJsonChanged
                ? objectMapper.convertValue(schema.getSchemaJson(), new TypeReference<Map<String, Object>>() {})
                : null;

        String oldName = schema.getName();
        mockSchemaMapper.updateEntityFromRequest(request, schema);

        // If name changed, update slug
        if (request.getName() != null && !request.getName().equals(oldName)) {
            String newSlug = slugService.generateSlug(request.getName());
            // Only conflict if a DIFFERENT schema already owns this slug
            mockSchemaRepository.findBySlugAndProjectId(newSlug, schema.getProject().getId())
                    .filter(conflict -> !conflict.getId().equals(schema.getId()))
                    .ifPresent(conflict -> {
                        throw new DuplicateResourceException("Schema slug already exists in this project");
                    });
            schema.setSlug(newSlug);
            endpointService.updateEndpointSlug(schema.getId(), "schema", newSlug);
        }

        if (isJsonChanged) {
            Integer currentVersion = schema.getActiveVersion();

            // If version 1 snapshot is missing (schema was created before versioning was added),
            // backfill it using previousJson — the state just before this edit.
            if (!mockSchemaVersionRepository.existsByMockSchemaIdAndVersion(schema.getId(), 1)) {
                MockSchemaVersion v1 = MockSchemaVersion.builder()
                        .mockSchema(schema)
                        .version(1)
                        .schemaJsonSnapshot(previousJson)
                        .diffJson(null)
                        .commitMessage("Initial schema creation")
                        .changedByUserId(userId)
                        .build();
                schema.getVersions().add(v1);
            }

            Integer nextVersion = currentVersion + 1;

            // Generate RFC 6902 json patch operations
            JsonNode patchNode = diffEngine.calculateRfc6902Patch(previousJson, schema.getSchemaJson());

            // Convert patch JsonNode to a List for JSONB column storage (RFC 6902 patches are arrays)
            List<Map<String, Object>> diffData = objectMapper.convertValue(patchNode, new TypeReference<List<Map<String, Object>>>() {});

            // Deep-copy current schema json for the immutable snapshot
            Map<String, Object> snapshotMap = objectMapper.convertValue(schema.getSchemaJson(), new TypeReference<Map<String, Object>>() {});

            String commitMessage = request.getCommitMessage() != null
                    ? request.getCommitMessage().trim()
                    : "Updated schema JSON definition";

            attachVersion(schema, nextVersion, snapshotMap, diffData, commitMessage, userId);
        }


        log.info("Schema {} updated to version {} by user {}", schemaId, schema.getActiveVersion(), userId);
        MockSchema saved = mockSchemaRepository.save(schema);
        return mockSchemaMapper.toResponse(saved);
    }

    /*
     Delete schema permanently
    */
    @Override
    @Transactional
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'DELETE')")
    public void deleteSchema(UUID userId, UUID schemaId) {
        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));
        log.warn("Schema {} deleted by user {}", schemaId, userId);
        mockSchemaRepository.delete(schema);
    }

    /*
     Return total schema count (for admin/stats)
    */
    @Override
    public long countSchemas() {
        return mockSchemaRepository.count();
    }

    /**
     * Builds a new {@link MockSchemaVersion} and attaches it to the schema's
     * managed collection so JPA cascade/orphanRemoval handles persistence.
     * Extracted to avoid duplicating this construction inline.
     */
    private void attachVersion(MockSchema schema, Integer version, Map<String, Object> snapshot,
                               List<Map<String, Object>> diff, String commitMessage, UUID changedByUserId) {
        MockSchemaVersion versionRecord = MockSchemaVersion.builder()
                .mockSchema(schema)
                .version(version)
                .schemaJsonSnapshot(snapshot)
                .diffJson(diff)
                .commitMessage(commitMessage)
                .changedByUserId(changedByUserId)
                .build();

        schema.getVersions().add(versionRecord);
        schema.setActiveVersion(version);
    }
}
