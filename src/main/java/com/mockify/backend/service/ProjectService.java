package com.mockify.backend.service;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectResponse;

import java.util.List;

public interface ProjectService {

    // Create new project under an organization
    ProjectResponse createProject(Long userId, CreateProjectRequest request);

    // Get all projects under a specific organization
    List<ProjectResponse> getProjectsByOrganizationId(Long userId, Long organizationId);

    // Fetch project details by ID
    ProjectResponse getProjectById(Long userId, Long projectId);

    // Update project name or info
    ProjectResponse updateProject(Long userId, Long projectId, UpdateProjectRequest request);

    // Delete project by ID
    void deleteProject(Long userId, Long projectId);

    // Count total projects
    long countProjects();
}
