package com.mockify.backend.controller;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectResponse;
import com.mockify.backend.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    // 🔹 Create a new project under an organization
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestHeader("userId") Long userId,
            @Valid @RequestBody CreateProjectRequest request) {

        log.info("User ID {}: Creating new project '{}' for organization ID {}",
                userId, request.getName(), request.getOrganizationId());

        ProjectResponse response = projectService.createProject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 🔹 Get all projects under a specific organization
    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByOrganization(
            @RequestHeader("userId") Long userId,
            @PathVariable Long organizationId) {

        log.info("User ID {}: Fetching all projects for organization ID {}", userId, organizationId);
        List<ProjectResponse> responses = projectService.getProjectsByOrganizationId(userId, organizationId);
        return ResponseEntity.ok(responses);
    }

    // 🔹 Get project by ID
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @RequestHeader("userId") Long userId,
            @PathVariable Long projectId) {

        log.info("User ID {}: Fetching project details for ID {}", userId, projectId);
        ProjectResponse response = projectService.getProjectById(userId, projectId);
        return ResponseEntity.ok(response);
    }

    // 🔹 Update project
    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @RequestHeader("userId") Long userId, // <-- Added userId
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {

        log.info("User ID {}: Updating project ID {} with data: {}", userId, projectId, request);
        ProjectResponse updated = projectService.updateProject(userId, projectId, request);
        return ResponseEntity.ok(updated);
    }

    // 🔹 Delete project
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @RequestHeader("userId") Long userId,
            @PathVariable Long projectId) {

        log.info("User ID {}: Deleting project ID {}", userId, projectId);
        projectService.deleteProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }

    // 🔹 Count total projects
    @GetMapping("/count")
    public ResponseEntity<Long> countProjects() {
        long count = projectService.countProjects();
        log.info("Total number of projects: {}", count);
        return ResponseEntity.ok(count);
    }
}
