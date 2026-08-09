package com.mockify.backend.dto.response.sandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
@Builder
public class SandboxSchemaInfo {
    private UUID schemaId;
    private String name;
    private String slug;

    /** Full public URL to fetch records for this schema. */
    private String apiUrl;

    /** Number of pre-seeded records created at sandbox initialization. */
    private int seedRecordCount;
}