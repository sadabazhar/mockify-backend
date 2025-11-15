package com.mockify.backend.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedException extends BaseException {

    public AccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
