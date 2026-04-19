package com.mockify.backend.dto.request.apikey;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateApiKeyRequest {

    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private Boolean isActive;

    private LocalDateTime expiresAt;

    @Min(value = 1, message = "Rate limit must be at least 1")
    @Max(value = 100000, message = "Rate limit must not exceed 100000")
    private Integer rateLimitPerMinute;
}