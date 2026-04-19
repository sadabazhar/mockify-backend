package com.mockify.backend.controller;

import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.dto.response.schema.SchemaTemplateResponse;
import com.mockify.backend.security.SecurityUtils;
import com.mockify.backend.service.EndpointService;
import com.mockify.backend.service.SchemaTemplateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Schema template endpoints.
 *
 * <p>{@code applyTemplate} requires SCHEMA:WRITE permission on the target project.
 * Authorization is enforced by {@code @PreAuthorize} on
 * {@link SchemaTemplateService#applyTemplateToProject}, backed by
 * {@link com.mockify.backend.security.MockifyPermissionEvaluator}. Both JWT
 * and API key callers are permitted provided they hold the required permission.</p>
 *
 * <p>{@code getSystemTemplates} is fully public — no authentication required.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/schema-templates")
@RequiredArgsConstructor
@Tag(name = "Mock Schema Templates")
public class SchemaTemplateController {

    private final EndpointService endpointService;
    private final SchemaTemplateService schemaTemplateService;

    /**
     * Public, built-in templates shared across all organizations.
     * No authentication required.
     */
    @GetMapping("/system")
    public ResponseEntity<List<SchemaTemplateResponse>> getSystemTemplates() {
        return ResponseEntity.ok(schemaTemplateService.getSystemTemplates());
    }

    /**
     * Apply a system template to a specific project.
     *
     * <p>Requires SCHEMA:WRITE on the resolved project.
     * Both JWT sessions and API keys with the required permission are accepted.</p>
     */
    @PostMapping("/{org}/{project}/{templateSlug}")
    public ResponseEntity<MockSchemaResponse> applyTemplate(
            @PathVariable String org,
            @PathVariable String project,
            @PathVariable String templateSlug,
            Authentication auth) {

        UUID userId = SecurityUtils.resolveUserId(auth);
        UUID projectId = endpointService.resolveProject(org, project);

        MockSchemaResponse response =
                schemaTemplateService.applyTemplateToProject(userId, projectId, templateSlug);

        // applying a template results in a new schema being created for the project.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}