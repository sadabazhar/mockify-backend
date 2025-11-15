package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.OrganizationMapper;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.OrganizationRepository;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.service.AccessControlService;
import com.mockify.backend.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMapper organizationMapper;
    private final AccessControlService accessControlService;

    // Create new organization under current user
    @Transactional
    @Override
    public OrganizationResponse createOrganization(Long userId, CreateOrganizationRequest request) {
        log.info("Creating organization '{}' for user ID {}", request.getName(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Prevent duplicate organization name under same user
        boolean exists = organizationRepository.findByOwnerId(userId).stream()
                .anyMatch(org -> org.getName().equalsIgnoreCase(request.getName()));
        if (exists) {
            throw new BadRequestException("Organization with name '" + request.getName() + "' already exists for this user.");
        }

        Organization organization = organizationMapper.toEntity(request);
        organization.setOwner(user);

        Organization saved = organizationRepository.save(organization);
        log.info("Organization '{}' created successfully (ID: {})", saved.getName(), saved.getId());

        return organizationMapper.toResponse(saved);
    }

    // Fetch organization details by ID
    @Transactional(readOnly = true)
    @Override
    public OrganizationResponse getOrganizationById(Long orgId) {
        log.debug("Fetching organization with ID: {}", orgId);
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));
        return organizationMapper.toResponse(organization);
    }

    // Get all organizations owned by current user
    @Transactional(readOnly = true)
    @Override
    public List<OrganizationResponse> getMyOrganizations(Long userId) {
        log.debug("Fetching organizations for user ID: {}", userId);
        List<Organization> organizations = organizationRepository.findByOwnerId(userId);
        return organizationMapper.toResponseList(organizations);
    }

    // Update organization name or details
    @Transactional
    @Override
    public OrganizationResponse updateOrganization(Long userId, Long orgId, UpdateOrganizationRequest request) {
        log.info("Updating organization ID {} for user ID {}", orgId, userId);

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));

        // Verify ownership
        accessControlService.checkOrganizationAccess(userId, organization, "Organization");

        // Prevent duplicate name under same user
        boolean nameExists = organizationRepository.findByOwnerId(userId).stream()
                .anyMatch(org -> !org.getId().equals(orgId)
                        && org.getName().equalsIgnoreCase(request.getName()));
        if (nameExists) {
            throw new BadRequestException("Another organization with name '" + request.getName() + "' already exists for this user.");
        }

        organizationMapper.updateEntityFromRequest(request, organization);
        Organization updated = organizationRepository.save(organization);

        log.info("Organization ID {} updated successfully", orgId);
        return organizationMapper.toResponse(updated);
    }

    // Delete organization
    @Transactional
    @Override
    public void deleteOrganization(Long userId, Long orgId) {
        log.warn("Deleting organization ID {} by user ID {}", orgId, userId);

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));

        // Verify ownership
        accessControlService.checkOrganizationAccess(userId, organization, "Organization");

        organizationRepository.delete(organization);
        log.info("Organization ID {} deleted successfully", orgId);
    }

    // Count total organizations
    @Override
    public long countOrganizations() {
        return organizationRepository.count();
    }
}
