package com.mockify.backend.dto.response.schema;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockSchemaResponse {
    private Long id;
    private String name;
    private Long projectId;
    private String schemaDefinition;
    private LocalDateTime createdAt;
}
