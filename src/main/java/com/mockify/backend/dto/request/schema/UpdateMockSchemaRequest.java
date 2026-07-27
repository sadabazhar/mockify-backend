package com.mockify.backend.dto.request.schema;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class UpdateMockSchemaRequest {
    @Size(min = 1, max = 255)
    private String name;
    private Map<String, Object> schemaJson;
    @Size(max = 500)
    private String commitMessage;
}
