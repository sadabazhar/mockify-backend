package com.mockify.backend.dto.request.schema;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateMockSchemaRequest {
    private String name;
    private Long projectId;
    private String schemaDefinition;
}
