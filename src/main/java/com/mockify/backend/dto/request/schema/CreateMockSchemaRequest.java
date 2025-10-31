package com.mockify.backend.dto.request.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CreateMockSchemaRequest {
    private String name;
    private Long projectId;
    private Map<String, Object> schemaJson;
}
