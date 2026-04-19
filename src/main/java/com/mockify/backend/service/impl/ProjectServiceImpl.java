package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectDetailResponse;
import com.mockify.backend.dto.response.project.ProjectResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.ProjectMapper;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.OrganizationRepository;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.ProjectService;
import com.mockify.backend.service.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectMapper projectMapper;
    private final SlugService slugService;
    private final EndpointService endpointService;

    // Create a new project under a specific organization Only
    // org-level keys or JWT owners only may create new projects.
    @Override
    @Transactional
    @PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'PROJECT:WRITE')")
    public ProjectResponse createProject(UUID userId, UUID orgId, CreateProjectRequest request) {
        log.info("User {} creating project '{}' under organization {}", userId, request.getName(), orgId);

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));

        // Generate slug from name
        String slug = slugService.generateSlug(request.getName());

        // Check slug uniqueness (organizations must have unique project-slugs)
        if (projectRepository.existsBySlugAndOrganizationId(slug, orgId)) {
            slug = slugService.generateUniqueSlug(slug);
        }

        // Check for duplicate project name in same organization
        Project existing = projectRepository.findByNameAndOrganizationId(request.getName(), orgId);
        if (existing != null) {
            throw new BadRequestException("Project with the same name already exists under this organization.");
        }

        Project project = projectMapper.toEntity(request);
        project.setOrganization(organization);
        project.setSlug(slug);

        Project saved = projectRepository.save(project);
        endpointService.createEndpoint(saved);

        log.info("Project '{}' created in org {} by user {}", saved.getName(), orgId, userId);
        return projectMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#organizationId, 'ORGANIZATION', 'PROJECT:READ')")
    public List<ProjectResponse> getProjectsByOrganizationId(UUID userId, UUID organizationId) {
        log.debug("User {} fetching projects for organization {}", userId, organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        List<Project> projects = projectRepository.findByOrganizationId(organizationId);
        return projectMapper.toResponseList(projects);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'READ')")
    public ProjectDetailResponse getProjectById(UUID userId, UUID projectId) {
        log.debug("User {} fetching project with ID {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        return projectMapper.toDetailResponse(project);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'WRITE')")
    public ProjectResponse updateProject(UUID userId, UUID projectId, UpdateProjectRequest request) {
        log.info("User {} updating project with ID {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        // Validate duplicate project name
        if (request.getName() != null) {
            Project existing = projectRepository.findByNameAndOrganizationId(request.getName(), project.getOrganization().getId());
            if (existing != null && !existing.getId().equals(projectId)) {
                throw new BadRequestException("Project name already exists in this organization.");
            }
        }

        String oldName = project.getName();
        projectMapper.updateEntityFromRequest(request, project);

        // If name changed, update slug
        if (request.getName() != null && !request.getName().equals(oldName)) {
            String newSlug = slugService.generateSlug(request.getName());
            if (projectRepository.existsBySlugAndOrganizationId(newSlug, project.getOrganization().getId())) {
                throw new DuplicateResourceException("Project slug already exists in this organization");
            }
            project.setSlug(newSlug);
            endpointService.updateEndpointSlug(project.getId(), "project", newSlug);
        }

        Project updated = projectRepository.save(project);
        log.info("Project '{}' updated by user {}", updated.getName(), userId);
        return projectMapper.toResponse(updated);
    }

    @Override
    @Transactional
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'DELETE')")
    public void deleteProject(UUID userId, UUID projectId) {
        log.info("User {} deleting project with ID {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));
        projectRepository.delete(project);
        log.warn("Project {} deleted by user {}", projectId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProjects() {
        long count = projectRepository.count();
        log.debug("Total projects count: {}", count);
        return count;
    }
}