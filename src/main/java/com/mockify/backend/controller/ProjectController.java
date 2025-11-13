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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    //  Create a new project under an organization
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.info("User {} creating new project '{}' under organization {}", userId, request.getName(), request.getOrganizationId());

        ProjectResponse created = projectService.createProject(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    //  Get a project by ID
    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.debug("User {} fetching project details for ID {}", userId, projectId);

        ProjectResponse project = projectService.getProjectById(userId, projectId);
        return ResponseEntity.ok(project);
    }

    //  Get all projects under a specific organization
    @GetMapping("/organizations/{organizationId}/projects")
    public ResponseEntity<List<ProjectResponse>> getProjectsByOrganization(
            @PathVariable Long organizationId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.debug("User {} fetching all projects under organization {}", userId, organizationId);

        List<ProjectResponse> projects = projectService.getProjectsByOrganizationId(userId, organizationId);
        return ResponseEntity.ok(projects);
    }

    // Update an existing project
    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.info("User {} updating project ID {} with new data: {}", userId, projectId, request);

        ProjectResponse updated = projectService.updateProject(userId, projectId, request);
        return ResponseEntity.ok(updated);
    }

    //  Delete a project
    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.warn("User {} deleting project ID {}", userId, projectId);

        projectService.deleteProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }

    // Count total projects
    @GetMapping("/projects/count")
    public ResponseEntity<Long> countProjects() {
        long count = projectService.countProjects();
        log.info("Total number of projects in the system: {}", count);
        return ResponseEntity.ok(count);
    }
}
