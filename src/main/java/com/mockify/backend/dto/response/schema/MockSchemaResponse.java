package com.mockify.backend.dto.response.schema;

import jakarta.persistence.Version;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockSchemaResponse {
    private UUID id;
    private String name;
    private String slug;
    private UUID projectId;
    private String projectName;
    private Map<String, Object> schemaJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int recordCount;
    private String endpointUrl;
    private Integer activeVersion;
    @Version
    private Long lockVersion;
}