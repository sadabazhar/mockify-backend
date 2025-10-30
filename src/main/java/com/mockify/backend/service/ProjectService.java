package com.mockify.backend.service;


import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectResponse;

import java.util.List;

public interface ProjectService {

    // Create new project under an organization
    ProjectResponse createProject(CreateProjectRequest request);

    // Get all projects under a specific organization
    List<ProjectResponse> getProjectsByOrganizationId(Long organizationId);

    // Fetch project details by ID
    ProjectResponse getProjectById(Long projectId);

    // Update project name or info
    ProjectResponse updateProject(Long projectId, UpdateProjectRequest request);

    // Delete project by ID
    void deleteProject(Long projectId);

    // Count total projects
    long countProjects();
}
