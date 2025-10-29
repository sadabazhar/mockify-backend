package com.mockify.backend.dto.response.organization;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private Long id;
    private String name;
    private Long ownerId;
    private LocalDateTime createdAt;
}
