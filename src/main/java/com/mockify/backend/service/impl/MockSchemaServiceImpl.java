package com.mockify.backend.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaDetailResponse;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockSchemaMapper;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.MockSchemaService;
import com.mockify.backend.service.MockValidatorService;
import com.mockify.backend.service.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

        MockSchema saved = mockSchemaRepository.save(schema);
        endpointService.createEndpoint(saved);

        log.info("Schema '{}' created in project {} by user {}", saved.getName(), projectId, userId);
        return mockSchemaMapper.toResponse(saved);
    }

    // Fetch all schemas under a project
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'SCHEMA:READ')")
    public List<MockSchemaResponse> getSchemasByProjectId(UUID userId, UUID projectId) {
        List<MockSchema> schemas = mockSchemaRepository.findByProjectId(projectId);
        return mockSchemaMapper.toResponseList(schemas);
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

        String oldName = schema.getName();
        mockSchemaMapper.updateEntityFromRequest(request, schema);

        // If name changed, update slug
        if (request.getName() != null && !request.getName().equals(oldName)) {
            String newSlug = slugService.generateSlug(request.getName());
            if (mockSchemaRepository.existsBySlugAndProjectId(newSlug, schema.getProject().getId())) {
                throw new DuplicateResourceException("Schema slug already exists in this project");
            }
            schema.setSlug(newSlug);
            endpointService.updateEndpointSlug(schema.getId(), "schema", newSlug);
        }

        log.info("Schema {} updated by user {}", schemaId, userId);
        mockSchemaRepository.save(schema);
        return mockSchemaMapper.toResponse(schema);
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
}