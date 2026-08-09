package com.mockify.backend.dto.response.sandbox;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SandboxResumeResult {
    private String accessToken;
    private long expiresIn;
}