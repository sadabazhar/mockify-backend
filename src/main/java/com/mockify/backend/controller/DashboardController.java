package com.mockify.backend.controller;

import com.mockify.backend.dto.response.dashboard.*;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Dashboard statistics endpoints.
 *
 * <p>Both JWT and API key callers are allowed. Resource-level authorization
 * (org / project / schema ownership) is enforced by {@code @PreAuthorize}
 * annotations on {@link DashboardService} methods, backed by
 * {@link com.mockify.backend.security.MockifyPermissionEvaluator}.</p>
 *
 * <p>Note: {@code userStats} and {@code recordHealth} are always scoped
 * to the caller's own user ID and carry no cross-user risk.</p>
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/user")
    public ResponseEntity<UserStats> userStats(Authentication auth) {
        UUID userId = SecurityUtils.resolveUserId(auth);
        return ResponseEntity.ok(dashboardService.userStats(userId));
    }

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<OrganizationStats> organizationStats(
            @PathVariable UUID orgId,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        return ResponseEntity.ok(dashboardService.organizationStats(userId, orgId));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ProjectStats> projectStats(
            @PathVariable UUID projectId,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        return ResponseEntity.ok(dashboardService.projectStats(userId, projectId));
    }

    @GetMapping("/schema/{schemaId}")
    public ResponseEntity<SchemaStats> schemaStats(
            @PathVariable UUID schemaId,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        return ResponseEntity.ok(dashboardService.schemaStats(userId, schemaId));
    }

    @GetMapping("/records/health")
    public ResponseEntity<RecordHealthStats> recordHealth(Authentication auth) {
        UUID userId = SecurityUtils.resolveUserId(auth);
        return ResponseEntity.ok(dashboardService.recordHealth(userId));
    }
}