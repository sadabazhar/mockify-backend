package com.mockify.backend.dto.response.project;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private Long organizationId;
    private LocalDateTime createdAt;
}
