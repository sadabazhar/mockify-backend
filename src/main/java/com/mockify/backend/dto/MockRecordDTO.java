package com.mockify.backend.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class MockRecordDTO {
    private Long id;
    private Long mockSchemaId;
    private Map<String, Object> data;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}