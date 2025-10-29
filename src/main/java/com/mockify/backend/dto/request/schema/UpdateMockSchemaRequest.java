package com.mockify.backend.dto.request.schema;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMockSchemaRequest {
    private String name;
    private String schemaDefinition;
}
