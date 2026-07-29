package com.mockify.backend.dto.response.schema;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockSchemaVersionSummaryResponse {
    private Integer version;
    private String commitMessage;
    private UUID changedByUserId;
    private LocalDateTime createdAt;
}
