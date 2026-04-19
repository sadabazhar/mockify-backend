package com.mockify.backend.service.impl;

import com.mockify.backend.dto.response.dashboard.*;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.repository.*;
import com.mockify.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordRepository mockRecordRepository;

    /**
     * User-scoped aggregate. Returns stats for resources owned by the caller.
     * No resource guard needed — the query is already filtered by userId.
     */
    @Override
    public UserStats userStats(UUID userId) {
        return dashboardRepository.userStats(userId);
    }

    /**
     * Org-level stats. Requires #READ on the organization.
     * JWT callers: org ownership. API key callers: ORGANIZATION:READ permission.
     *
     * <p>The {@code userId} parameter is unused after authorization and exists
     * only for API compatibility. It will be removed when the service interface
     * is cleaned up.</p>
     */
    @Override
    @PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'READ')")
    public OrganizationStats organizationStats(UUID userId, UUID orgId) {
        // @PreAuthorize has already verified org access — no need to load+check here.
        organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));
        return dashboardRepository.organizationStats(orgId);
    }

    /**
     * Project-level stats. Requires #READ on the project.
     */
    @Override
    @PreAuthorize("hasPermission(#projectId, 'PROJECT', 'READ')")
    public ProjectStats projectStats(UUID userId, UUID projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        LocalDateTime now = LocalDateTime.now();
        return dashboardRepository.projectStats(projectId, now);
    }

    /**
     * Schema-level stats. Requires #READ on the schema.
     */
    @Override
    @PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'READ')")
    public SchemaStats schemaStats(UUID userId, UUID schemaId) {
        mockSchemaRepository.findById(schemaId)
                .orElseThrow(() -> new ResourceNotFoundException("Schema not found"));
        LocalDateTime now = LocalDateTime.now();
        return dashboardRepository.schemaStats(schemaId, now, now.plusMinutes(60));
    }

    /**
     * Record health stats filtered by the caller's own user ID.
     * No resource guard needed — the query cannot expose other users' data.
     */
    @Override
    public RecordHealthStats recordHealth(UUID userId) {
        LocalDateTime now = LocalDateTime.now();
        return dashboardRepository.recordHealthStats(userId, now, now.plusMinutes(60));
    }
}