package com.mockify.backend.controller;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationDetailResponse;
import com.mockify.backend.dto.response.organization.OrganizationResponse;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.OrganizationService;
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
@Tag(name = "Organization")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final EndpointService endpointService;


    /**
     * Create a new organization.
     * SECURITY: JWT-only
     */
    @PostMapping("/organizations")
    public ResponseEntity<OrganizationResponse> createOrganization(
            Authentication auth,
            @Valid @RequestBody CreateOrganizationRequest request) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        log.info("User ID {} creating organization: {}", userId, request.getName());

        OrganizationResponse response = organizationService.createOrganization(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get details of a specific organization.
     * SECURITY: API keys allowed (read-only)
     */
    @GetMapping("/organizations/{org}")
    public ResponseEntity<OrganizationDetailResponse> getOrganization(
            Authentication auth,
            @PathVariable String org) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId = endpointService.resolveOrganization(org);

        log.debug("User {} fetching organization {}", userId, orgId);

        OrganizationDetailResponse response = organizationService.getOrganizationDetail(orgId, userId);
        return ResponseEntity.ok(response);
    }


    /**
     * List all organizations for the authenticated user/key owner.
     * SECURITY: API keys allowed (read-only)
     */
    @GetMapping("/organizations")
    public ResponseEntity<List<OrganizationResponse>> getMyOrganizations(Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        List<OrganizationResponse> responses = organizationService.getMyOrganizations(userId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Update organization metadata (name, settings, etc.).
     * SECURITY: JWT-only
     */
    @PutMapping("/organizations/{org}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            Authentication auth,
            @PathVariable String org,
            @Valid @RequestBody UpdateOrganizationRequest request) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId = endpointService.resolveOrganization(org);

        OrganizationResponse updated = organizationService.updateOrganization(userId, orgId, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete an organization and all its resources.
     * SECURITY: JWT-only
     */
    @DeleteMapping("/organizations/{org}")
    public ResponseEntity<Void> deleteOrganization(
            Authentication auth,
            @PathVariable String org) {

        SecurityUtils.requireJwtAuthentication(auth);

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID orgId = endpointService.resolveOrganization(org);

        organizationService.deleteOrganization(userId, orgId);
        return ResponseEntity.noContent().build();
    }
}