package com.mockify.backend.service.impl;

import com.mockify.backend.exception.DuplicateResourceException;
import com.mockify.backend.exception.ResourceNotFoundException;
import com.mockify.backend.model.*;
import com.mockify.backend.repository.EndpointRepository;
import com.mockify.backend.service.EndpointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointServiceImpl implements EndpointService {

    private final EndpointRepository endpointRepository;

    @Override
    @Transactional
    public void createEndpoint(Organization organization) {
        String fullPath = organization.getSlug();

        if (endpointRepository.existsByFullPath(fullPath)) {
            throw new DuplicateResourceException("Endpoint path already exists: " + fullPath);
        }

        Endpoint endpoint = new Endpoint();
        endpoint.setFullPath(fullPath);
        endpoint.setSlug(organization.getSlug());
        endpoint.setResourceType(Endpoint.ResourceType.ORGANIZATION);
        endpoint.setParentEndpoint(null); // Organizations are root level
        endpoint.setOrganization(organization);

        if (endpointRepository.existsByFullPath(fullPath)) {
            throw new DuplicateResourceException("Endpoint already exists: " + fullPath);
        }
        endpointRepository.save(endpoint);

        log.debug("Created organization endpoint: {}", fullPath);
    }

    @Override
    @Transactional
    public void createEndpoint(Project project) {
        // Find parent organization endpoint
        Endpoint orgEndpoint = endpointRepository
                .findByOrganizationId(project.getOrganization().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent organization endpoint not found"
                ));

        String fullPath = Endpoint.buildFullPath(orgEndpoint, project.getSlug());

        Endpoint endpoint = new Endpoint();
        endpoint.setFullPath(fullPath);
        endpoint.setSlug(project.getSlug());
        endpoint.setResourceType(Endpoint.ResourceType.PROJECT);
        endpoint.setParentEndpoint(orgEndpoint);
        endpoint.setProject(project);

        if (endpointRepository.existsByFullPath(fullPath)) {
            throw new DuplicateResourceException("Endpoint already exists: " + fullPath);
        }
        endpointRepository.save(endpoint);

        log.debug("Created project endpoint: {}", fullPath);
    }

    @Override
    @Transactional
    public void createEndpoint(MockSchema schema) {
        // Find parent project endpoint
        Endpoint projectEndpoint = endpointRepository
                .findByProjectId(schema.getProject().getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent project endpoint not found"
                ));

        String fullPath = Endpoint.buildFullPath(projectEndpoint, schema.getSlug());

        Endpoint endpoint = new Endpoint();
        endpoint.setFullPath(fullPath);
        endpoint.setSlug(schema.getSlug());
        endpoint.setResourceType(Endpoint.ResourceType.SCHEMA);
        endpoint.setParentEndpoint(projectEndpoint);
        endpoint.setSchema(schema);

        if (endpointRepository.existsByFullPath(fullPath)) {
            throw new DuplicateResourceException("Endpoint already exists: " + fullPath);
        }
        endpointRepository.save(endpoint);

        log.debug("Created schema endpoint: {}", fullPath);
    }

    @Override
    @Transactional
    public void updateEndpointSlug(UUID resourceId, String resourceType, String newSlug) {
        Endpoint endpoint = findEndpointByResource(resourceId, resourceType);

        // Build new full path
        String newFullPath;
        if (endpoint.getParentEndpoint() == null) {
            newFullPath = newSlug;
        } else {
            newFullPath = Endpoint.buildFullPath(endpoint.getParentEndpoint(), newSlug);
        }

        // Check for conflicts — only if a DIFFERENT endpoint already owns this path
        endpointRepository.findByFullPath(newFullPath)
                .filter(conflict -> !conflict.getId().equals(endpoint.getId()))
                .ifPresent(conflict -> {
                    throw new DuplicateResourceException("Endpoint path already exists: " + newFullPath);
                });

        String oldFullPath = endpoint.getFullPath();
        endpoint.setSlug(newSlug);
        endpoint.setFullPath(newFullPath);
        endpointRepository.save(endpoint);

        // Update all child endpoints recursively
        updateChildPaths(endpoint, oldFullPath, newFullPath);

        log.debug("Updated endpoint slug from {} to {}", oldFullPath, newFullPath);
    }

    /**
     * Recursively update all child endpoint paths when parent changes
     */
    private void updateChildPaths(Endpoint parent, String oldParentPath, String newParentPath) {

        // Load direct children
        var children = endpointRepository.findByParentEndpoint(parent);

        for (Endpoint child : children) {
            String oldChildPath = child.getFullPath();

            // Replace only the parent prefix
            String newChildPath = oldChildPath.replaceFirst(
                    "^" + java.util.regex.Pattern.quote(oldParentPath),
                    newParentPath
            );

            // Update child path
            child.setFullPath(newChildPath);
            endpointRepository.save(child);

            log.debug("Updated child endpoint path: {} → {}", oldChildPath, newChildPath);

            // Recurse into grandchildren
            updateChildPaths(child, oldChildPath, newChildPath);
        }
    }

    @Override
    @Transactional
    public void deleteEndpoint(UUID resourceId, String resourceType) {
        Endpoint endpoint = findEndpointByResource(resourceId, resourceType);

        if (endpoint != null) {
            endpointRepository.delete(endpoint);
            log.debug("Deleted endpoint: {}", endpoint.getFullPath());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolveOrganization(String orgSlug) {
        Endpoint endpoint = endpointRepository.findByFullPath(orgSlug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found: " + orgSlug
                ));

        if (endpoint.getResourceType() != Endpoint.ResourceType.ORGANIZATION) {
            throw new ResourceNotFoundException(
                    "Path does not point to an organization: " + orgSlug
            );
        }

        return endpoint.getOrganization().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolveProject(String orgSlug, String projectSlug) {
        String fullPath = orgSlug + "/" + projectSlug;

        Endpoint endpoint = endpointRepository.findByFullPath(fullPath)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found: " + fullPath
                ));

        if (endpoint.getResourceType() != Endpoint.ResourceType.PROJECT) {
            throw new ResourceNotFoundException(
                    "Path does not point to a project: " + fullPath
            );
        }

        return endpoint.getProject().getId();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID resolveSchema(String orgSlug, String projectSlug, String schemaSlug) {
        String fullPath = orgSlug + "/" + projectSlug + "/" + schemaSlug;

        Endpoint endpoint = endpointRepository.findByFullPath(fullPath)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Schema not found: " + fullPath
                ));

        if (endpoint.getResourceType() != Endpoint.ResourceType.SCHEMA) {
            throw new ResourceNotFoundException(
                    "Path does not point to a schema: " + fullPath
            );
        }

        return endpoint.getSchema().getId();
    }

    /**
     * Helper to find endpoint by resource ID and type
     */
    private Endpoint findEndpointByResource(UUID resourceId, String resourceType) {
        return switch (resourceType.toLowerCase()) {
            case "organization" -> endpointRepository.findByOrganizationId(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));
            case "project" -> endpointRepository.findByProjectId(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));
            case "schema" -> endpointRepository.findBySchemaId(resourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Endpoint not found"));
            default -> throw new IllegalArgumentException("Invalid resource type: " + resourceType);
        };
    }
}