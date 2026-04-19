package com.mockify.backend.security;

import com.mockify.backend.model.ApiKeyPermission.ApiPermission;
import com.mockify.backend.model.ApiKeyPermission.ApiResourceType;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.MockRecordRepository;
import com.mockify.backend.repository.MockSchemaRepository;
import com.mockify.backend.repository.OrganizationRepository;
import com.mockify.backend.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.UUID;

/**
 * Central authorization engine for all resource-level access decisions.
 *
 * <p>This is the single place where "can this caller do this to this resource?"
 * is answered. It is invoked by Spring AOP whenever a service method annotated
 * with {@code @PreAuthorize("hasPermission(...)") } is called.</p>
 *
 * <h2>SpEL call convention</h2>
 * <pre>
 *   hasPermission(targetId, targetType, permission)
 * </pre>
 * <ul>
 *   <li>{@code targetId}   — UUID of the resource (or its container for collection ops)</li>
 *   <li>{@code targetType} — resource entity to load: {@code "SCHEMA"}, {@code "RECORD"},
 *                            {@code "PROJECT"}, {@code "ORGANIZATION"}</li>
 *   <li>{@code permission} — one of:
 *     <ul>
 *       <li>Simple:   {@code "READ"}, {@code "WRITE"}, {@code "DELETE"} — permission on the
 *                     loaded resource itself</li>
 *       <li>Compound: {@code "SCHEMA:WRITE"}, {@code "RECORD:READ"}, {@code "PROJECT:READ"} —
 *                     permission on a child resource type <em>within</em> the loaded container.
 *                     Used for collection-level operations (e.g. "can this key create schemas
 *                     in project X?").</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h2>Authorization paths</h2>
 * <dl>
 *   <dt>JWT caller</dt>
 *   <dd>Organisation ownership check. A valid JWT session grants full owner-level
 *       access to all resources within the owned organisation.</dd>
 *   <dt>API key caller</dt>
 *   <dd>Three sequential guards:
 *     <ol>
 *       <li><b>Org scope</b>  — key must belong to the same org as the resource.</li>
 *       <li><b>Project scope</b> — a project-scoped key is rejected for resources
 *           outside its project.</li>
 *       <li><b>Permission level</b> — the key must carry the required {@link ApiPermission}
 *           on the right {@link ApiResourceType}, optionally scoped to a specific resource ID.
 *           The hierarchy ADMIN ⊇ DELETE ⊇ WRITE ⊇ READ is respected.</li>
 *     </ol>
 *   </dd>
 * </dl>
 *
 * <h2>Usage examples</h2>
 * <pre>
 *   // Read a specific schema
 *   &#64;PreAuthorize("hasPermission(#schemaId, 'SCHEMA', 'READ')")
 *
 *   // Create a schema inside a project (collection op — no schema ID yet)
 *   &#64;PreAuthorize("hasPermission(#projectId, 'PROJECT', 'SCHEMA:WRITE')")
 *
 *   // Delete a record
 *   &#64;PreAuthorize("hasPermission(#recordId, 'RECORD', 'DELETE')")
 *
 *   // Read org details
 *   &#64;PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'READ')")
 * </pre>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MockifyPermissionEvaluator implements PermissionEvaluator {

    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordRepository mockRecordRepository;

    // -------------------------------------------------------------------------
    // PermissionEvaluator contract
    // -------------------------------------------------------------------------

    /**
     * Primary entry point — invoked by {@code hasPermission(targetId, targetType, permission)}.
     *
     * @param authentication the caller's authentication token (never null here)
     * @param targetId       UUID of the resource or container
     * @param targetType     entity type string (case-insensitive)
     * @param permission     permission string — simple ("READ") or compound ("SCHEMA:WRITE")
     */
    @Override
    public boolean hasPermission(
            Authentication authentication,
            Serializable targetId,
            String targetType,
            Object permission) {

        if (authentication == null || targetId == null || targetType == null || permission == null) {
            return false;
        }

        UUID resourceId = toUUID(targetId);
        if (resourceId == null) {
            log.warn("hasPermission called with non-UUID targetId: {}", targetId);
            return false;
        }

        String permStr = permission.toString().toUpperCase();

        try {
            return evaluate(authentication, resourceId, targetType.toUpperCase(), permStr);
        } catch (Exception e) {
            log.error("Permission evaluation error: type={}, id={}, perm={}", targetType, targetId, permission, e);
            return false;
        }
    }

    /**
     * Secondary entry point — invoked by {@code hasPermission(domainObject, permission)}.
     * Not used in this project; all annotations pass an ID string.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        log.warn("hasPermission(domainObject, permission) called — use hasPermission(id, type, permission) instead");
        return false;
    }

    // -------------------------------------------------------------------------
    // Core evaluation
    // -------------------------------------------------------------------------

    private boolean evaluate(Authentication auth, UUID targetId, String targetType, String permission) {

        // Parse permission — may be simple ("WRITE") or compound ("SCHEMA:WRITE")
        String resourcePermStr;
        String resourceTypeOverride; // non-null only for compound permissions
        if (permission.contains(":")) {
            String[] parts = permission.split(":", 2);
            resourceTypeOverride = parts[0]; // child resource type (e.g. "SCHEMA")
            resourcePermStr = parts[1];      // permission level  (e.g. "WRITE")
        } else {
            resourceTypeOverride = null;
            resourcePermStr = permission;
        }

        ApiPermission requiredPermission = parsePermission(resourcePermStr);
        if (requiredPermission == null) {
            log.warn("Unknown permission string: {}", resourcePermStr);
            return false;
        }

        // Load the target entity and resolve org + project context
        ResourceContext ctx = loadContext(targetId, targetType);
        if (ctx == null) {
            return false; // resource not found — deny
        }

        // For compound permissions, the resource type to check is the child type, not the container
        ApiResourceType resourceType = parseResourceType(
                resourceTypeOverride != null ? resourceTypeOverride : targetType
        );
        if (resourceType == null) {
            log.warn("Unknown resource type: {}", targetType);
            return false;
        }

        if (auth instanceof ApiKeyAuthenticationToken token) {
            return evaluateApiKey(token, ctx, resourceType, requiredPermission, targetId, resourceTypeOverride != null);
        } else {
            return evaluateJwt(auth, ctx);
        }
    }

    // -------------------------------------------------------------------------
    // JWT path — org ownership is sufficient for all operations
    // -------------------------------------------------------------------------

    private boolean evaluateJwt(Authentication auth, ResourceContext ctx) {
        UUID callerId = resolveJwtUserId(auth);
        if (callerId == null) {
            return false;
        }

        boolean owns = ctx.organization().getOwner().getId().equals(callerId);
        if (!owns) {
            log.debug("JWT access denied: user {} does not own org {}", callerId, ctx.organization().getId());
        }
        return owns;
    }

    // -------------------------------------------------------------------------
    // API key path — three sequential guards
    // -------------------------------------------------------------------------

    private boolean evaluateApiKey(
            ApiKeyAuthenticationToken token,
            ResourceContext ctx,
            ApiResourceType resourceType,
            ApiPermission requiredPermission,
            UUID targetId,
            boolean isCollectionOp
    ) {
        // Guard 1: organization scope
        if (!token.hasOrganizationAccess(ctx.organization().getId())) {
            log.warn("API key {} denied: org mismatch (key={}, resource={})",
                    token.getApiKeyId(), token.getOrganizationId(), ctx.organization().getId());
            return false;
        }

        // Guard 2: project scope
        if (ctx.projectId() != null && !token.hasProjectAccess(ctx.projectId())) {
            log.warn("API key {} denied: project scope mismatch (key scoped to={}, resource project={})",
                    token.getApiKeyId(), token.getProjectId(), ctx.projectId());
            return false;
        }

        // Guard 3: permission level on resource type
        // For collection ops (compound permission), targetId is the container — pass null
        // so wildcard permissions match; a key with SCHEMA:WRITE on any schema in the org can create.
        UUID permissionTargetId = isCollectionOp ? null : targetId;
        boolean granted = token.hasPermission(resourceType, requiredPermission, permissionTargetId);

        if (!granted) {
            log.warn("API key {} denied: missing {}:{} on resource {}",
                    token.getApiKeyId(), resourceType, requiredPermission, permissionTargetId);
        }
        return granted;
    }

    // -------------------------------------------------------------------------
    // Resource loading — resolves org + project context from any entity
    // -------------------------------------------------------------------------

    private ResourceContext loadContext(UUID id, String targetType) {
        return switch (targetType) {
            case "ORGANIZATION" -> {
                Organization org = organizationRepository.findByIdWithOwnerAndProjects(id).orElse(null);
                yield org != null ? new ResourceContext(org, null) : null;
            }
            case "PROJECT" -> {
                Project project = projectRepository.findByIdWithOrgAndOwner(id).orElse(null);
                yield project != null ? new ResourceContext(project.getOrganization(), project.getId()) : null;
            }
            case "SCHEMA" -> {
                MockSchema schema = mockSchemaRepository.findWithContextById(id).orElse(null);
                yield schema != null
                        ? new ResourceContext(schema.getProject().getOrganization(), schema.getProject().getId())
                        : null;
            }
            case "RECORD" -> {
                MockRecord record = mockRecordRepository.findWithContextById(id).orElse(null);
                yield record != null
                        ? new ResourceContext(
                        record.getMockSchema().getProject().getOrganization(),
                        record.getMockSchema().getProject().getId())
                        : null;
            }
            default -> {
                log.warn("loadContext: unknown targetType '{}'", targetType);
                yield null;
            }
        };
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ApiPermission parsePermission(String s) {
        try {
            return ApiPermission.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ApiResourceType parseResourceType(String s) {
        try {
            return ApiResourceType.valueOf(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private UUID resolveJwtUserId(Authentication auth) {
        if (auth.getPrincipal() instanceof UserDetails ud) {
            try {
                return UUID.fromString(ud.getUsername());
            } catch (IllegalArgumentException e) {
                log.warn("JWT principal username is not a UUID: {}", ud.getUsername());
                return null;
            }
        }
        return null;
    }

    private UUID toUUID(Serializable id) {
        if (id instanceof UUID uuid) return uuid;
        try {
            return UUID.fromString(id.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Carries the organization and optional project resolved from a target resource.
     * Used to evaluate both JWT ownership and API key scope checks.
     */
    private record ResourceContext(Organization organization, UUID projectId) {}
}