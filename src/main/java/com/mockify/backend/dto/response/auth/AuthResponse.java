package com.mockify.backend.dto.response.auth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String status;
    private String message;
    private TokenInfo tokens;
    private UserResponse user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenInfo {
        private String accessToken;
        private String tokenType;
        private Long expiresIn;
    }
}
