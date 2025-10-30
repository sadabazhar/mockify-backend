package com.mockify.backend.service;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationResponse;

import java.util.List;
import java.util.Optional;

//Handles all CRUD operations related to organizations.
public interface OrganizationService {

    // Create new organization under current user
    OrganizationResponse createOrganization(CreateOrganizationRequest request);

    // Fetch organization details by ID
    OrganizationResponse getOrganizationById(Long orgId);

    // Get all organizations owned by current user
    List<OrganizationResponse> getMyOrganizations();

    // Update organization name or details
    OrganizationResponse updateOrganization(Long orgId, UpdateOrganizationRequest request);

    // Delete organization (and optionally its related projects)
    void deleteOrganization(Long orgId);

    // Count total organizations in system
    long countOrganizations();
}
