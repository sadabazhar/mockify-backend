package com.mockify.backend.service;

import com.mockify.backend.model.Organization;
import com.mockify.backend.security.MockifyPermissionEvaluator;

import java.util.UUID;

/**
 * Ownership checks used by JWT-only service paths.
 *
 * <p><b>Migration note — Phase 3 and 4:</b> Resource-level authorization for
 * dual-auth endpoints (JWT + API key) is being migrated to declarative
 * {@code @PreAuthorize("hasPermission(...)")} annotations backed by
 * {@link MockifyPermissionEvaluator}. As each service is migrated the
 * corresponding {@code checkOrganizationAccess} calls will be removed,
 * and this interface will eventually be deleted entirely.</p>
 */
public interface AccessControlService {

    /**
     * Verifies the caller owns the given organization.
     *
     * <p>Used only by JWT-authenticated management endpoints that have not yet
     * been migrated to {@code @PreAuthorize}. Do not add new callers.</p>
     *
     * @throws com.mockify.backend.exception.AccessDeniedException if ownership check fails
     */
    @Deprecated(forRemoval = true)
    void checkOrganizationAccess(UUID userId, Organization organization, String resourceName);
}