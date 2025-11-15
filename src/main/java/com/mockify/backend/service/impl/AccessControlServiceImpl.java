package com.mockify.backend.service.impl;

import com.mockify.backend.exception.ForbiddenException;
import com.mockify.backend.model.Organization;
import com.mockify.backend.service.AccessControlService;
import org.springframework.stereotype.Service;

@Service
public class AccessControlServiceImpl implements AccessControlService {

    @Override
    public void checkOrganizationAccess(Long userId, Organization organization, String resourceName) {

        // Current rule: only owner has access
        if (!organization.getOwner().getId().equals(userId)) {
            throw new ForbiddenException(
                    "You do not have permission to access this " + resourceName
            );
        }
    }
}
