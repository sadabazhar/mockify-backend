package com.mockify.backend.dto.response.record;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockRecordResponse {
    private Long id;
    private Long schemaId;
    private Map<String, Object> data;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
