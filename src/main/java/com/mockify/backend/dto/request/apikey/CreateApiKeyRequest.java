package com.mockify.backend.dto.request.apikey;

import com.mockify.backend.model.ApiKeyPermission.ApiPermission;
import com.mockify.backend.model.ApiKeyPermission.ApiResourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyRequest {

    @NotBlank(message = "API key name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private UUID projectId;

    @Future(message = "Expiry time must be in the future")
    private LocalDateTime expiresAt;

    @Min(value = 1, message = "Rate limit must be at least 1")
    @Max(value = 100000, message = "Rate limit must not exceed 100000")
    private Integer rateLimitPerMinute = 1000;

    @Valid
    @NotEmpty(message = "At least one permission is required")
    private List<PermissionRequest> permissions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PermissionRequest {

        @NotNull(message = "Permission level is required")
        private ApiPermission permission;

        @NotNull(message = "Resource type is required")
        private ApiResourceType resourceType;

        private UUID resourceId;
    }
}