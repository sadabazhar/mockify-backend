package com.mockify.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MockSchemaDTO {
    private Long id;
    private String name;
    private Long projectId;
    private Map<String, Object> schemaJson;
    private LocalDateTime createdAt;
}
