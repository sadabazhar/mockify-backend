package com.mockify.backend.dto.request.record;

import lombok.Getter;
import lombok.Setter;


import java.util.Map;

@Getter
@Setter
public class UpdateMockRecordRequest {
    private Map<String, Object> data;

}
