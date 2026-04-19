package com.mockify.backend.controller;

import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectDetailResponse;
import com.mockify.backend.dto.response.project.ProjectResponse;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Project")
public class ProjectController {

    private final ProjectService projectService;
    private final EndpointService endpointService;

    /**
     * Create a new project under an organization.
     * SECURITY: JWT-only
     */
    @PostMapping("/{org}/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable String org,
            @Valid @RequestBody CreateProjectRequest request,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId = endpointService.resolveOrganization(org);
        log.info("User {} creating new project '{}' under organization {}", userId, request.getName(), org);

        ProjectResponse created = projectService.createProject(userId, orgId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a single project's details.
     * SECURITY: API keys allowed (read-only)
     */
    @GetMapping("/{org}/{project}")
    public ResponseEntity<ProjectDetailResponse> getProject(
            @PathVariable String org,
            @PathVariable String project,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID projectId = endpointService.resolveProject(org, project);
        log.debug("User {} fetching project details for ID {}", userId, projectId);

        ProjectDetailResponse response = projectService.getProjectById(userId, projectId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing project.
     * SECURITY: JWT-only
     */
    @PutMapping("/{org}/{project}")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable String org,
            @PathVariable String project,
            @Valid @RequestBody UpdateProjectRequest request,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID projectId = endpointService.resolveProject(org, project);
        log.info("User {} updating project {}", userId, projectId);

        ProjectResponse updated = projectService.updateProject(userId, projectId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a project and all its resources.
     * SECURITY: JWT-only
     */
    @DeleteMapping("/{org}/{project}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable String org,
            @PathVariable String project,
            Authentication auth) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID projectId = endpointService.resolveProject(org, project);
        log.warn("User {} deleting project ID {}", userId, projectId);

        projectService.deleteProject(userId, projectId);
        return ResponseEntity.noContent().build();
    }

    /**
     * List all projects under a specific organization.
     * SECURITY: API keys allowed (read-only)
     */
    @GetMapping("/{org}/projects")
    public ResponseEntity<List<ProjectResponse>> getProjectsByOrganization(
            @PathVariable String org,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID organizationId = endpointService.resolveOrganization(org);
        log.debug("User {} fetching projects under org {}", userId, organizationId);

        List<ProjectResponse> projects = projectService.getProjectsByOrganizationId(userId, organizationId);
        return ResponseEntity.ok(projects);
    }
}