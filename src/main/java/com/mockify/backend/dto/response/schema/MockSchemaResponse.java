package com.mockify.backend.dto.response.schema;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockSchemaResponse {
    private Long id;
    private String name;
    private Long projectId;
    private Map<String, Object> schemaJson;
    private LocalDateTime createdAt;
}
