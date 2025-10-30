package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectResponse;
import com.mockify.backend.service.ProjectService;
import org.springframework.stereotype.Service;

import java.util.List;

//Handles project management inside organizations.
@Service
public class ProjectServiceImpl implements ProjectService {


    // Create new project under an organization
    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {
        return null;
    }

    // Get all projects under a specific organization
    @Override
    public List<ProjectResponse> getProjectsByOrganizationId(Long organizationId) {
        return List.of();
    }

    // Fetch project details by ID
    @Override
    public ProjectResponse getProjectById(Long projectId) {
        return null;
    }

    // Update project name or info
    @Override
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request) {
        return null;
    }

    // Delete project by ID
    @Override
    public void deleteProject(Long projectId) {

    }

    // Count total projects
    @Override
    public long countProjects() {
        return 0;
    }
}
