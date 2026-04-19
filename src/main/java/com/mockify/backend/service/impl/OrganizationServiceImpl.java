package com.mockify.backend.service.impl;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationDetailResponse;
import com.mockify.backend.dto.response.organization.OrganizationResponse;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.mapper.OrganizationMapper;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.User;
import com.mockify.backend.repository.OrganizationRepository;
import com.mockify.backend.repository.UserRepository;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.OrganizationService;
import com.mockify.backend.service.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizationMapper organizationMapper;
    private final SlugService slugService;
    private final EndpointService endpointService;

    // Create new organization under current user
    // The JWT-only guard at the controller (requireJwtAuthentication) is sufficient.
    @Transactional
    @Override
    public OrganizationResponse createOrganization(UUID userId, CreateOrganizationRequest request) {
        log.info("Creating organization '{}' for user ID {}", request.getName(), userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Generate slug from name
        String slug = slugService.generateSlug(request.getName());

        // Check global slug uniqueness (organizations must have globally unique slugs)
        if (organizationRepository.existsBySlug(slug)) {
            slug = slugService.generateUniqueSlug(slug);
        }

        // Prevent duplicate organization name under same user
        boolean exists = organizationRepository.findByOwnerId(userId).stream()
                .anyMatch(org -> org.getName().equalsIgnoreCase(request.getName()));
        if (exists) {
            throw new BadRequestException("Organization '" + request.getName() + "' already exists for this user.");
        }

        Organization organization = organizationMapper.toEntity(request);
        organization.setOwner(user);
        organization.setSlug(slug);

        Organization saved = organizationRepository.save(organization);
        endpointService.createEndpoint(saved);

        log.info("Organization '{}' created by user {}", saved.getName(), userId);
        return organizationMapper.toResponse(saved);
    }


    // Fetch organization details by ID.
    // Public read — no auth check needed (returns non-sensitive summary).
    @Transactional(readOnly = true)
    @Override
    public OrganizationResponse getOrganizationById(UUID orgId) {
        log.debug("Fetching organization with ID: {}", orgId);
        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));
        return organizationMapper.toResponse(organization);
    }

    // Returns full detail including projects — restricted to the owner.
    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'READ')")
    public OrganizationDetailResponse getOrganizationDetail(UUID orgId, UUID userId) {
        log.debug("Fetching organization detail: {}", orgId);

        Organization organization = organizationRepository.findByIdWithOwnerAndProjects(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + orgId));
        return organizationMapper.toDetailResponse(organization);
    }

    // Returns only the caller's own orgs — no cross-org risk, no guard needed.
    @Transactional(readOnly = true)
    @Override
    public List<OrganizationResponse> getMyOrganizations(UUID userId) {
        log.debug("Fetching organizations for user ID: {}", userId);
        List<Organization> organizations = organizationRepository.findByOwnerId(userId);
        return organizationMapper.toResponseList(organizations);
    }

    // Update organization name or details
    @Transactional
    @Override
    @PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'WRITE')")
    public OrganizationResponse updateOrganization(UUID userId, UUID orgId, UpdateOrganizationRequest request) {
        log.info("Updating organization ID {} for user ID {}", orgId, userId);

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));

        // Prevent duplicate name under same user
        boolean nameExists = organizationRepository.findByOwnerId(userId).stream()
                .anyMatch(org -> !org.getId().equals(orgId)
                        && org.getName().equalsIgnoreCase(request.getName()));
        if (nameExists) {
            throw new BadRequestException("Another organization named '" + request.getName() + "' already exists.");
        }

        // If name changed, update slug
        String oldName = organization.getName();
        organizationMapper.updateEntityFromRequest(request, organization);

        if (request.getName() != null && !request.getName().equals(oldName)) {
            String newSlug = slugService.generateSlug(request.getName());
            if (organizationRepository.existsBySlug(newSlug)) {
                throw new DuplicateResourceException("Organization slug already exists");
            }
            organization.setSlug(newSlug);
            endpointService.updateEndpointSlug(organization.getId(), "organization", newSlug);
        }

        Organization updated = organizationRepository.save(organization);
        log.info("Organization {} updated by user {}", orgId, userId);
        return organizationMapper.toResponse(updated);
    }

    // Delete organization
    @Transactional
    @Override
    @PreAuthorize("hasPermission(#orgId, 'ORGANIZATION', 'DELETE')")
    public void deleteOrganization(UUID userId, UUID orgId) {
        log.warn("Deleting organization ID {} by user ID {}", orgId, userId);

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with ID: " + orgId));
        organizationRepository.delete(organization);
        log.warn("Organization {} deleted by user {}", orgId, userId);
    }

    // Count total organizations
    @Override
    public long countOrganizations() {
        return organizationRepository.count();
    }
}