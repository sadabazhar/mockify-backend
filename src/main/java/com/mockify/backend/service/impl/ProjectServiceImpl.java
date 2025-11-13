package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.ProjectMapper;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.OrganizationRepository;
import com.mockify.backend.repository.ProjectRepository;
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

    // Create new project under an organization
    @Transactional
    @Override
    public ProjectResponse createProject(Long userId, CreateProjectRequest request) { // <-- Added userId parameter
        log.info("User ID {}: Creating project '{}' under organization ID {}", userId, request.getName(), request.getOrganizationId());

        Organization org = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + request.getOrganizationId()));

        // Prevent duplicate project names within same organization
        boolean exists = projectRepository.findByOrganizationId(org.getId()).stream()
                .anyMatch(p -> p.getName().equalsIgnoreCase(request.getName()));
        if (exists) {
            throw new BadRequestException("Project with name '" + request.getName() + "' already exists in this organization.");
        }

        Project project = projectMapper.toEntity(request);
        project.setOrganization(org);

        Project saved = projectRepository.save(project);
        log.info("User ID {}: Project '{}' created successfully (ID: {})", userId, saved.getName(), saved.getId());

        return projectMapper.toResponse(saved);
    }

    // Update existing project
    @Transactional
    @Override
    public ProjectResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request) {
        log.info("User ID {}: Updating project ID {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        // Prevent duplicate name within the same organization
        boolean nameExists = projectRepository.findByOrganizationId(project.getOrganization().getId()).stream()
                .anyMatch(p -> !p.getId().equals(projectId)
                        && p.getName().equalsIgnoreCase(request.getName()));
        if (nameExists) {
            throw new BadRequestException("Another project with name '" + request.getName() + "' already exists in this organization.");
        }

        projectMapper.updateEntityFromRequest(request, project);
        Project updated = projectRepository.save(project);
        log.info("User ID {}: Project ID {} updated successfully", userId, projectId);

        return projectMapper.toResponse(updated);
    }

    // Fetch project by ID
    @Transactional(readOnly = true)
    @Override
    public ProjectResponse getProjectById(Long userId, Long projectId) {
        log.debug("User ID {}: Fetching project with ID: {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        return projectMapper.toResponse(project);
    }

    // Get all projects under a specific organization
    @Transactional(readOnly = true)
    @Override
    public List<ProjectResponse> getProjectsByOrganizationId(Long userId, Long organizationId) {
        log.debug("User ID {}: Fetching projects for organization ID: {}", userId, organizationId);

        if (!organizationRepository.existsById(organizationId)) {
            throw new ResourceNotFoundException("Organization not found with ID: " + organizationId);
        }

        List<Project> projects = projectRepository.findByOrganizationId(organizationId);
        return projectMapper.toResponseList(projects);
    }

    // Delete project by ID
    @Transactional
    @Override
    public void deleteProject(Long userId, Long projectId) {
        log.warn("User ID {}: Deleting project with ID: {}", userId, projectId);

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + projectId));

        projectRepository.delete(project);
        log.info("User ID {}: Project ID {} deleted successfully", userId, projectId);
    }

    // Count total projects
    @Override
    public long countProjects() {
        return projectRepository.count();
    }
}
