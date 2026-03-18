package com.mockify.backend.dto.request.record;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class AutoGenerateRequest {
    @Min(1)
    @Max(1000)
    private int count;


}
