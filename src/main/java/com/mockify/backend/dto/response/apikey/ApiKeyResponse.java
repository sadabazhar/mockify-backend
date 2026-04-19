package com.mockify.backend.dto.response.apikey;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyResponse {

    private UUID id;
    private String name;
    private String description;
    private String keyPrefix;
    private UUID organizationId;
    private String organizationName;
    private UUID projectId;
    private String projectName;
    private UUID createdBy;
    private String createdByName;
    private boolean isActive;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer rateLimitPerMinute;
    private List<PermissionResponse> permissions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PermissionResponse {
        private UUID id;
        private String permission;
        private String resourceType;
        private UUID resourceId;
        private String resourceName;
    }
}