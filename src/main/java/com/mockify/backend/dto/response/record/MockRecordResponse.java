package com.mockify.backend.dto.response.record;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockRecordResponse {
    private Long id;
    private Long schemaId;
    private String data;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
