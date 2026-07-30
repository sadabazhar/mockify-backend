package com.mockify.backend.exception;

public class DiffGenerationException extends RuntimeException {

    public DiffGenerationException(String message) {
        super(message);
    }

    public DiffGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}