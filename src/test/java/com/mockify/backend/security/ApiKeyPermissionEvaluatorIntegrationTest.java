package com.mockify.backend.security;

import com.mockify.backend.model.*;
import com.mockify.backend.model.ApiKeyPermission.ApiPermission;
import com.mockify.backend.model.ApiKeyPermission.ApiResourceType;
import com.mockify.backend.repository.*;
import com.mockify.backend.service.MockSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end test proving that ApiKeyAuthenticationToken flows correctly
 * through MockifyPermissionEvaluator to a @PreAuthorize-annotated service.
 *
 * The test sets up a real ApiKeyAuthenticationToken in the SecurityContext
 * (exactly as ApiKeyAuthenticationFilter does in production) and calls
 * a protected service method, verifying all three evaluator guards fire:
 *
 *   Guard 1 — org scope:     key from a different org is denied
 *   Guard 2 — project scope: project-scoped key is denied for resources
 *                             outside its project
 *   Guard 3 — permission:    key with READ is denied when WRITE is required;
 *                             key with correct permission is granted
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ApiKeyPermissionEvaluatorIntegrationTest {

    @Autowired MockSchemaService mockSchemaService;
    @Autowired MockSchemaRepository mockSchemaRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired UserRepository userRepository;
    @Autowired ApiKeyRepository apiKeyRepository;

    private User owner;
    private Organization org;
    private Organization otherOrg;
    private Project project;
    private Project otherProject;
    private MockSchema schema;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(buildUser("evaluator-test@test.com"));

        org = organizationRepository.save(buildOrg("Evaluator Test Org", owner));
        otherOrg = organizationRepository.save(buildOrg("Other Org", owner));

        project = projectRepository.save(buildProject("Test Project", org));
        otherProject = projectRepository.save(buildProject("Other Project", org));

        schema = mockSchemaRepository.save(buildSchema("Test Schema", project));
    }

    // -------------------------------------------------------------------------
    // Guard 1 — org scope
    // -------------------------------------------------------------------------

    @Test
    void getSchemaById_apiKey_wrongOrg_denied() {
        // Key belongs to otherOrg — schema belongs to org
        authenticateAsApiKey(otherOrg.getId(), null, List.of(
                buildPermission(ApiPermission.READ, ApiResourceType.SCHEMA, null)
        ));

        assertThatThrownBy(() ->
                mockSchemaService.getSchemaById(owner.getId(), schema.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------------------------------------------------------------------------
    // Guard 2 — project scope
    // -------------------------------------------------------------------------

    @Test
    void getSchemaById_apiKey_projectScopedToOtherProject_denied() {
        // Key is scoped to otherProject — schema lives in project
        authenticateAsApiKey(org.getId(), otherProject.getId(), List.of(
                buildPermission(ApiPermission.READ, ApiResourceType.SCHEMA, null)
        ));

        assertThatThrownBy(() ->
                mockSchemaService.getSchemaById(owner.getId(), schema.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getSchemaById_apiKey_orgScopedKey_passesProjectScopeGuard() {
        // Org-level key (projectId = null) — should pass Guard 2
        authenticateAsApiKey(org.getId(), null, List.of(
                buildPermission(ApiPermission.READ, ApiResourceType.SCHEMA, null)
        ));

        assertThatNoException().isThrownBy(() ->
                mockSchemaService.getSchemaById(owner.getId(), schema.getId()));
    }

    // -------------------------------------------------------------------------
    // Guard 3 — permission level
    // -------------------------------------------------------------------------

    @Test
    void getSchemaById_apiKey_correctOrgAndReadPermission_granted() {
        authenticateAsApiKey(org.getId(), null, List.of(
                buildPermission(ApiPermission.READ, ApiResourceType.SCHEMA, null)
        ));

        assertThatNoException().isThrownBy(() ->
                mockSchemaService.getSchemaById(owner.getId(), schema.getId()));
    }

    @Test
    void deleteSchema_apiKey_readOnlyPermission_denied() {
        // Has READ but needs DELETE
        authenticateAsApiKey(org.getId(), null, List.of(
                buildPermission(ApiPermission.READ, ApiResourceType.SCHEMA, null)
        ));

        assertThatThrownBy(() ->
                mockSchemaService.deleteSchema(owner.getId(), schema.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteSchema_apiKey_adminPermission_granted() {
        // ADMIN includes DELETE via ApiPermission.includes()
        authenticateAsApiKey(org.getId(), null, List.of(
                buildPermission(ApiPermission.ADMIN, ApiResourceType.SCHEMA, null)
        ));

        assertThatNoException().isThrownBy(() ->
                mockSchemaService.deleteSchema(owner.getId(), schema.getId()));
    }

    @Test
    void getSchemaById_apiKey_correctOrgNoPermissions_denied() {
        // Right org, right project scope, but no permissions at all
        authenticateAsApiKey(org.getId(), null, List.of());

        assertThatThrownBy(() ->
                mockSchemaService.getSchemaById(owner.getId(), schema.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getSchemaById_apiKey_scopedToCorrectProject_granted() {
        // Project-scoped key for the right project — should pass all three guards
        authenticateAsApiKey(org.getId(), project.getId(), List.of(
                buildPermission(ApiPermission.READ, ApiResourceType.SCHEMA, null)
        ));

        assertThatNoException().isThrownBy(() ->
                mockSchemaService.getSchemaById(owner.getId(), schema.getId()));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Sets up the SecurityContext exactly as ApiKeyAuthenticationFilter does
     * in production — the evaluator sees an identical token during tests.
     */
    private void authenticateAsApiKey(UUID orgId, UUID projectId, List<ApiKeyPermission> permissions) {
        ApiKeyAuthenticationToken token = new ApiKeyAuthenticationToken(
                UUID.randomUUID(),   // apiKeyId
                owner.getId(),       // ownerId
                orgId,
                projectId,
                permissions,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_API_KEY"))
        );
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private ApiKeyPermission buildPermission(ApiPermission perm, ApiResourceType type, UUID resourceId) {
        ApiKeyPermission p = new ApiKeyPermission();
        p.setPermission(perm);
        p.setResourceType(type);
        p.setResourceId(resourceId);
        return p;
    }

    private User buildUser(String email) {
        User u = new User();
        u.setName("Test");
        u.setEmail(email);
        u.setPassword("hashed");
        u.setProviderName("local");
        u.setEmailVerified(true);
        return u;
    }

    private Organization buildOrg(String name, User owner) {
        Organization o = new Organization();
        o.setName(name);
        o.setSlug(name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID());
        o.setOwner(owner);
        return o;
    }

    private Project buildProject(String name, Organization org) {
        Project p = new Project();
        p.setName(name);
        p.setSlug(name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID());
        p.setOrganization(org);
        return p;
    }

    private MockSchema buildSchema(String name, Project project) {
        MockSchema s = new MockSchema();
        s.setName(name);
        s.setSlug(name.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID());
        s.setSchemaJson(Map.of("type", "object", "properties", Map.of()));
        s.setProject(project);
        return s;
    }
}