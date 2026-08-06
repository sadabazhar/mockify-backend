package com.mockify.backend.dto.response.schema;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockSchemaVersionResponse {
    private UUID id;
    private UUID schemaId;
    private Integer version;
    private Map<String, Object> schemaJsonSnapshot;
    private List<Map<String, Object>> diffJson;
    private String commitMessage;
    private UUID changedByUsername;
    private LocalDateTime createdAt;
    private UUID changedByUserId;
}
