package com.mockify.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ProjectDTO {
    private Long id;
    private String name;
    private Long organizationId;
    private LocalDateTime createdAt;
}