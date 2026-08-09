package com.mockify.backend.dto.response.sandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class SandboxWorkspace {

    /** Slug of the auto-created sandbox organization. */
    private String orgSlug;

    /** Slug of the auto-created demo project. */
    private String projectSlug;

    /** Pre-seeded schemas with their public API URLs. */
    private List<SandboxSchemaInfo> schemas;

    /**
     * Base URL for public mock access.
     * Pattern: /api/mock/{orgSlug}/{projectSlug}
     * Append /{schemaSlug}/records to get a ready-to-use endpoint.
     */
    private String baseMockUrl;
}