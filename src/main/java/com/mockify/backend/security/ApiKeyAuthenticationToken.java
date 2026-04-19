package com.mockify.backend.security;

import com.mockify.backend.model.ApiKeyPermission;
import com.mockify.backend.model.ApiKeyPermission.ApiPermission;
import com.mockify.backend.model.ApiKeyPermission.ApiResourceType;
import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Custom authentication token for API key-based authentication.
 *
 * <p>Carries org/project scope and the full list of {@link ApiKeyPermission}
 * rows loaded at authentication time. The permission list is used by
 * {@link MockifyPermissionEvaluator} to answer {@code hasPermission()} calls
 * without additional DB round-trips during request handling.</p>
 */
@Getter
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final UUID apiKeyId;
    private final UUID ownerId;           // createdBy UUID — used by controllers as effective user
    private final UUID organizationId;
    private final UUID projectId;         // null for org-level keys
    private final List<ApiKeyPermission> permissions;
    private final int rateLimitPerMinute;

    public ApiKeyAuthenticationToken(
            UUID apiKeyId,
            UUID ownerId,
            UUID organizationId,
            UUID projectId,
            List<ApiKeyPermission> permissions,
            Collection<? extends GrantedAuthority> authorities) {
        this(apiKeyId, ownerId, organizationId, projectId, permissions, authorities, 1000);
    }

    public ApiKeyAuthenticationToken(
            UUID apiKeyId,
            UUID ownerId,
            UUID organizationId,
            UUID projectId,
            List<ApiKeyPermission> permissions,
            Collection<? extends GrantedAuthority> authorities,
            int rateLimitPerMinute) {
        super(authorities);
        this.apiKeyId = apiKeyId;
        this.ownerId = ownerId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.permissions = permissions != null ? permissions : List.of();
        this.rateLimitPerMinute = rateLimitPerMinute;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // API key is not stored in token for security
    }

    @Override
    public Object getPrincipal() {
        return apiKeyId.toString();
    }

    // -------------------------------------------------------------------------
    // Scope helpers
    // -------------------------------------------------------------------------

    /** Returns true if the key is scoped to a specific project rather than the whole org. */
    public boolean isProjectScoped() {
        return projectId != null;
    }

    /**
     * Returns true if this key may access the given organization.
     * An org-level key or a project-scoped key both belong to exactly one org.
     */
    public boolean hasOrganizationAccess(UUID requestedOrgId) {
        return organizationId.equals(requestedOrgId);
    }

    /**
     * Returns true if this key may access the given project.
     * An org-level key (projectId == null) has access to all projects in its org.
     * A project-scoped key is restricted to its own project.
     */
    public boolean hasProjectAccess(UUID requestedProjectId) {
        if (projectId == null) {
            return true; // org-level key — all projects allowed
        }
        return projectId.equals(requestedProjectId);
    }

    // -------------------------------------------------------------------------
    // Permission check
    // -------------------------------------------------------------------------

    /**
     * Returns true if this key carries a permission that satisfies
     * {@code requiredPermission} on {@code resourceType}.
     *
     * <p>Match rules (all must hold):</p>
     * <ol>
     *   <li>The permission row's {@code resourceType} matches {@code resourceType}.</li>
     *   <li>The row's {@link ApiPermission#includes(ApiPermission)} returns true for
     *       {@code requiredPermission} (ADMIN ⊇ DELETE ⊇ WRITE ⊇ READ).</li>
     *   <li>The row's {@code resourceId} is either {@code null} (wildcard) or equals
     *       {@code resourceId} (when non-null, restricts to a specific resource).</li>
     * </ol>
     *
     * @param resourceType       the type of resource being accessed
     * @param requiredPermission the minimum permission level required
     * @param resourceId         the specific resource UUID, or {@code null} for collection ops
     */
    public boolean hasPermission(
            ApiResourceType resourceType,
            ApiPermission requiredPermission,
            UUID resourceId) {

        return permissions.stream().anyMatch(p ->
                p.getResourceType() == resourceType
                        && p.getPermission().includes(requiredPermission)
                        && (p.getResourceId() == null || p.getResourceId().equals(resourceId))
        );
    }
}