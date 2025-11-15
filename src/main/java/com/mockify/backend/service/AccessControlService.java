package com.mockify.backend.service;

import com.mockify.backend.model.Organization;

public interface AccessControlService {

    void checkOrganizationAccess(Long userId, Organization organization, String resourceName);
}
