package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ForbiddenException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.ProjectMapper;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.OrganizationRepository;
import com.mockify.backend.repository.ProjectRepository;
import com.mockify.backend.service.AccessControlService;
import com.mockify.backend.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectMapper projectMapper;
    private final AccessControlService accessControlService;

    @Override
    @Transactional
    public ProjectResponse createProject(Long userId, CreateProjectRequest request) {
        log.info("User {} creating project '{}' under organization {}", userId, request.getName(), request.getOrganizationId());

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + request.getOrganizationId()));

        // Ownership check
        accessControlService.checkOrganizationAccess(userId, organization, "Organization");

        // Check for duplicate project name in same organization
        Project existing = projectRepository.findByNameAndOrganizationId(request.getName(), request.getOrganizationId());
        if (existing != null) {
            throw new BadRequestException("Project with the same name already exists under this organization.");
        }

        Project project = projectMapper.toEntity(request);
        project.setOrganization(organization);

        Project saved = projectRepository.save(project);
        log.info("Project '{}' created successfully by user {}", saved.getName(), userId);
        return projectMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectResponse> getProjectsByOrganizationId(Long userId, Long organizationId) {
        log.debug("User {} fetching projects for organization {}", userId, organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));

        accessControlService.checkOrganizationAccess(userId, organization, "Organization");

        List<Project> projects = projectRepository.findByOrganizationId(organizationId);
        return projectMapper.toResponseList(projects);
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long userId, Long projectId) {
        log.debug("User {} fetching project with ID {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        accessControlService.checkOrganizationAccess(userId, project.getOrganization(), "Project");

        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        log.info("User {} updating project with ID {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        accessControlService.checkOrganizationAccess(userId, project.getOrganization(), "Project");

        // Validate duplicate project name
        if (request.getName() != null) {
            Project existing = projectRepository.findByNameAndOrganizationId(request.getName(), project.getOrganization().getId());
            if (existing != null && !existing.getId().equals(projectId)) {
                throw new BadRequestException("Project name already exists in this organization.");
            }
        }

        projectMapper.updateEntityFromRequest(request, project);
        Project updated = projectRepository.save(project);
        log.info("Project '{}' updated successfully by user {}", updated.getName(), userId);

        return projectMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteProject(Long userId, Long projectId) {
        log.info("User {} deleting project with ID {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId));

        accessControlService.checkOrganizationAccess(userId, project.getOrganization(), "Project");

        projectRepository.delete(project);
        log.info("Project with ID {} deleted successfully by user {}", projectId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countProjects() {
        long count = projectRepository.count();
        log.debug("Total projects count: {}", count);
        return count;
    }
}
