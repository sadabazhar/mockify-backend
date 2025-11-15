package com.mockify.backend.service.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ForbiddenException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.MockSchemaMapper;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.Project;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.service.MockSchemaService;
import com.mockify.backend.service.MockValidatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MockSchemaServiceImpl implements MockSchemaService {

    private final MockSchemaRepository mockSchemaRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final MockSchemaMapper mockSchemaMapper;
    private final ObjectMapper objectMapper;
    private final MockValidatorService mockValidatorService;

    // Utility method to fetch user or throw
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // Utility method to fetch project with ownership validation
    private Project getProjectWithAccessCheck(Long projectId, Long userId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));
        if (!project.getOrganization().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this project");
        }
        return project;
    }

    // Utility method to fetch schema with ownership validation
    private MockSchema getSchemaWithAccessCheck(Long schemaId, Long userId) {
        MockSchema schema = mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));
        if (!schema.getProject().getOrganization().getOwner().getId().equals(userId)) {
            throw new ForbiddenException("Access denied to this schema");
        }
        return schema;
    }

    /*
      Create a new mock schema under a specific project
      Only the organization owner can create schemas
     */
    @Override
    @Transactional
    public MockSchemaResponse createSchema(Long userId, CreateMockSchemaRequest request) {
        getUserOrThrow(userId);
        Project project = getProjectWithAccessCheck(request.getProjectId(), userId);

        // Prevent duplicate schema name in the same project
        boolean exists = mockSchemaRepository.findByNameAndProjectId(request.getName(), project.getId()) != null;
        if (exists) {
            throw new DuplicateResourceException("Schema with the same name already exists in this project");
        }

        // Validate Mock Schema
        mockValidatorService.validateSchemaDefinition(request.getSchemaJson());

        MockSchema schema = mockSchemaMapper.toEntity(request);
        schema.setProject(project);

        MockSchema saved = mockSchemaRepository.save(schema);
        return mockSchemaMapper.toResponse(saved);
    }

    /*
      Fetch all schemas under a project
      Only the org owner can view them
     */
    @Override
    @Transactional(readOnly = true)
    public List<MockSchemaResponse> getSchemasByProjectId(Long userId, Long projectId) {
        getUserOrThrow(userId);
        getProjectWithAccessCheck(projectId, userId);
        List<MockSchema> schemas = mockSchemaRepository.findByProjectId(projectId);
        return mockSchemaMapper.toResponseList(schemas);
    }

    /*
      Fetch a specific schema
     */
    @Override
    @Transactional(readOnly = true)
    public MockSchemaResponse getSchemaById(Long userId, Long schemaId) {
        MockSchema schema = getSchemaWithAccessCheck(schemaId, userId);
        return mockSchemaMapper.toResponse(schema);
    }

    /*
      Update schema (name or schema JSON)
      Ensures unique name and valid ownership
     */
    @Override
    @Transactional
    public MockSchemaResponse updateSchema(Long userId, Long schemaId, UpdateMockSchemaRequest request) {
        MockSchema schema = getSchemaWithAccessCheck(schemaId, userId);

        // Check if new name conflicts with another schema in same project
        if (request.getName() != null && !request.getName().equals(schema.getName())) {

            MockSchema existing = mockSchemaRepository.findByNameAndProjectId(
                    request.getName(),
                    schema.getProject().getId()
            );

            if (existing != null && !existing.getId().equals(schema.getId())) {
                throw new DuplicateResourceException("Schema with this name already exists");
            }
        }

        // Validate Mock Schema
        mockValidatorService.validateSchemaDefinition(request.getSchemaJson());

        mockSchemaMapper.updateEntityFromRequest(request, schema);
        mockSchemaRepository.save(schema);
        return mockSchemaMapper.toResponse(schema);
    }

    /*
      Delete schema permanently
     */
    @Override
    @Transactional
    public void deleteSchema(Long userId, Long schemaId) {
        MockSchema schema = getSchemaWithAccessCheck(schemaId, userId);
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
