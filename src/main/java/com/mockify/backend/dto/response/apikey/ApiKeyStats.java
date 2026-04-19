package com.mockify.backend.dto.response.apikey;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyStats {
    private long currentRequests;
    private long remainingRequests;
    private int rateLimitPerMinute;
    private long resetInSeconds;
}