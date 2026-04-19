package com.mockify.backend.dto.response.apikey;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateApiKeyResult {

    private String apiKey;
    private ApiKeyResponse keyInfo;

    @Builder.Default
    private String securityNotice = "Store this API key securely. It will not be shown again.";
}