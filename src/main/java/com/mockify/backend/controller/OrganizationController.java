package com.mockify.backend.controller;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationResponse;
import com.mockify.backend.service.OrganizationService;
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
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;

    // Create organization for logged-in user
    @PostMapping
    public ResponseEntity<OrganizationResponse> createOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrganizationRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());
        log.info("User ID {} creating organization: {}", userId, request.getName());

        OrganizationResponse response = organizationService.createOrganization(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Get details of a specific organization
    @GetMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> getOrganizationById(@PathVariable Long orgId) {
        return ResponseEntity.ok(organizationService.getOrganizationById(orgId));
    }

    // Get all organizations for current user
    @GetMapping
    public ResponseEntity<List<OrganizationResponse>> getMyOrganizations(
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        List<OrganizationResponse> responses = organizationService.getMyOrganizations(userId);
        return ResponseEntity.ok(responses);
    }

    // Update organization
    @PutMapping("/{orgId}")
    public ResponseEntity<OrganizationResponse> updateOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orgId,
            @Valid @RequestBody UpdateOrganizationRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());
        OrganizationResponse updated = organizationService.updateOrganization(userId, orgId, request);
        return ResponseEntity.ok(updated);
    }

    // Delete organization
    @DeleteMapping("/{orgId}")
    public ResponseEntity<Void> deleteOrganization(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orgId) {

        Long userId = Long.parseLong(userDetails.getUsername());
        organizationService.deleteOrganization(userId, orgId);
        return ResponseEntity.noContent().build();
    }
}
