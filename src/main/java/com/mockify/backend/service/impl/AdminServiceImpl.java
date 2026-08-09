package com.mockify.backend.service.impl;

import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.common.validation.PageableValidator;
import com.mockify.backend.dto.response.admin.AdminUserResponse;
import com.mockify.backend.dto.response.admin.AdminOrganizationResponse;
import com.mockify.backend.dto.response.admin.AdminProjectResponse;
import com.mockify.backend.dto.response.admin.AdminMockSchemaResponse;
import com.mockify.backend.dto.response.admin.AdminMockRecordResponse;
import com.mockify.backend.mapper.admin.*;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.model.Organization;
import com.mockify.backend.model.Project;
import com.mockify.backend.repository.*;
import com.mockify.backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ProjectRepository projectRepository;
    private final MockSchemaRepository mockSchemaRepository;
    private final MockRecordRepository mockRecordRepository;

    private final AdminUserMapper userMapper;
    private final AdminOrganizationMapper organizationMapper;
    private final AdminProjectMapper projectMapper;
    private final AdminMockSchemaMapper schemaMapper;
    private final AdminMockRecordMapper recordMapper;

    @Override
    public Page<AdminUserResponse> listUsers(String email, UserRole role, Pageable pageable) {
        PageableValidator.validate(pageable);

        // Always exclude GUEST accounts from admin user management.
        // Guests are ephemeral infrastructure; admins should see real accounts only.
        // The sandbox cleanup scheduler handles guest lifecycle separately.
        final UserRole excludedRole = UserRole.GUEST;

        if (email != null && role != null) {
            // Specific email + specific role (must not be GUEST to return results)
            if (role == UserRole.GUEST) {
                return Page.empty(pageable);
            }
            return userRepository
                    .findByEmailContainingIgnoreCaseAndRole(email, role, pageable)
                    .map(userMapper::toResponse);
        }

        if (email != null) {
            return userRepository
                    .findByEmailContainingIgnoreCaseAndRoleNot(email, excludedRole, pageable)
                    .map(userMapper::toResponse);
        }

        if (role != null) {
            if (role == UserRole.GUEST) {
                return Page.empty(pageable);
            }
            return userRepository
                    .findByRole(role, pageable)
                    .map(userMapper::toResponse);
        }

        log.debug("Admin fetching users page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        return userRepository
                .findByRoleNot(excludedRole, pageable)
                .map(userMapper::toResponse);
    }

    @Override
    public Page<AdminOrganizationResponse> listOrganizations(UUID userId, Pageable pageable) {

        // Validate page size, protect from abuse
        PageableValidator.validate(pageable);

        Page<Organization> page;

        // Admin can see all Org or specific Org
        if (userId != null) {
            page = organizationRepository.findByOwnerId(userId, pageable);
        } else {
            page = organizationRepository.findAll(pageable);
        }

        log.debug("Admin fetching Organization details page={}, size={}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        return page.map(organizationMapper::toResponse);
    }

    @Override
    public Page<AdminProjectResponse> listProjects(UUID orgId, Pageable pageable) {

        // Validate page size, protect from abuse
        PageableValidator.validate(pageable);

        Page<Project> page;

        // Admin can see all Projects or specific project under a Org
        if (orgId != null) {
            page = projectRepository.findByOrganizationId(orgId, pageable);
        } else {
            page = projectRepository.findAll(pageable);
        }

        log.debug("Admin fetching Project details page={}, size={}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        return page.map(projectMapper::toResponse);
    }

    @Override
    public Page<AdminMockSchemaResponse> listSchemas(UUID projectId, Pageable pageable) {

        // Validate page size, protect from abuse
        PageableValidator.validate(pageable);

        Page<MockSchema> page;

        // Admin can see all schema or specific schema under a project
        if (projectId != null) {
            page = mockSchemaRepository.findByProjectId(projectId, pageable);
        } else {
            page = mockSchemaRepository.findAll(pageable);
        }

        log.debug("Admin fetching schema details page={}, size={}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        return page.map(schemaMapper::toResponse);
    }

    @Override
    public Page<AdminMockRecordResponse> listRecords(UUID schemaId, Pageable pageable) {

        // Validate page size, protect from abuse
        PageableValidator.validate(pageable);

        Page<MockRecord> page;

        // Admin can see all records or specific records under a schema
        if (schemaId != null) {
            page = mockRecordRepository.findByMockSchema_Id(schemaId, pageable);
        } else {
            page = mockRecordRepository.findAll(pageable);
        }

        log.debug("Admin fetching record details page={}, size={}",
                pageable.getPageNumber(),
                pageable.getPageSize());

        return page.map(recordMapper::toResponse);
    }

    // TODO: Leter we add promote/demote users
}
