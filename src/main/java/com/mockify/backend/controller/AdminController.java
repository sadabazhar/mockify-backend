package com.mockify.backend.controller;

import com.mockify.backend.dto.response.admin.*;
import com.mockify.backend.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "Admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /*
         Basic admin health endpoint.
         Used to verify ADMIN access works.
     */
    @GetMapping("/ping")
    public Map<String, Object> adminPing(){
        return Map.of(
                "status", "ok",
                "message", "Admin access granted"
        );
    }

    // TODO: Add pagination (Page<T>) & filters (?ownerId=)

    @GetMapping("/users")
    public List<AdminUserResponse> users() {
        return adminService.listUsers();
    }

    @GetMapping("/organizations")
    public List<AdminOrganizationResponse> organizations() {
        return adminService.listOrganizations();
    }

    @GetMapping("/projects")
    public List<AdminProjectResponse> projects() {
        return adminService.listProjects();
    }

    @GetMapping("/schemas")
    public List<AdminMockSchemaResponse> schemas() {
        return adminService.listSchemas();
    }

    @GetMapping("/records")
    public List<AdminMockRecordResponse> records() {
        return adminService.listRecords();
    }

    // TODO: Add promote/demote users
}
