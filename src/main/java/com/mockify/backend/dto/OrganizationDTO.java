package com.mockify.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OrganizationDTO {
    private Long id;
    private String name;
    private Long ownerId; // Only expose owner ID, not full object info
    private LocalDateTime createdAt;
}
