package com.mockify.backend.dto.response.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /**
     * Machine-readable error code for frontend handling.
     * Null for most errors — only set when the frontend needs
     * to take a specific action beyond displaying the message.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorCode;

    /**
     * URL the frontend should direct the user to when errorCode is set.
     * Null for all standard errors.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String actionUrl;
}