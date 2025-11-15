package com.mockify.backend.dto.request.record;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CreateMockRecordRequest {
    private Long schemaId;
    private Map<String, Object> data;

}
