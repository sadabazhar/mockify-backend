package com.mockify.backend.dto.request.schema;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RollbackRequest {

    @NotNull(message = "Target version is required")
    @Min(value = 1, message = "Target version must be greater than 0")
    private Integer targetVersion;

    @Size(max = 500, message = "Rollback reason cannot exceed 500 characters")
    private String rollbackReason;
}
