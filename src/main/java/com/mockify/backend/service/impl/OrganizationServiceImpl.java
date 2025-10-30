package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationResponse;
import com.mockify.backend.service.OrganizationService;
import org.springframework.stereotype.Service;

import java.util.List;

//Handles all CRUD operations related to organizations.
@Service
public class OrganizationServiceImpl implements OrganizationService {


    // Create new organization under current user
    @Override
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        return null;
    }

    // Fetch organization details by ID
    @Override
    public OrganizationResponse getOrganizationById(Long orgId) {
        return null;
    }

    // Get all organizations owned by current user
    @Override
    public List<OrganizationResponse> getMyOrganizations() {
        return List.of();
    }

    // Update organization name or details
    @Override
    public OrganizationResponse updateOrganization(Long orgId, UpdateOrganizationRequest request) {
        return null;
    }

    // Delete organization (and optionally its related projects)
    @Override
    public void deleteOrganization(Long orgId) {

    }

    // Count total organizations in system
    @Override
    public long countOrganizations() {
        return 0;
    }








}
