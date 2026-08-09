package com.mockify.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a sandbox session has passed its hard expiry cap.
 *
 * HTTP 410 Gone is semantically correct here — the resource existed
 * and is now permanently gone, unlike 401 (unauthenticated) or
 * 403 (forbidden). The frontend intercepts 410 specifically to show
 * the "convert to keep your work" prompt rather than a generic error.
 */
public class SandboxExpiredException extends BaseException {

    public SandboxExpiredException() {
        super("Your sandbox session has expired. Convert your account to keep your work.", HttpStatus.GONE);
    }

    public SandboxExpiredException(String message) {
        super(message, HttpStatus.GONE);
    }
}