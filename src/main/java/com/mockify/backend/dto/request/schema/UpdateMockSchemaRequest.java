package com.mockify.backend.dto.request.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UpdateMockSchemaRequest {
    private String name;
    private Map<String, Object> schemaJson;
}
