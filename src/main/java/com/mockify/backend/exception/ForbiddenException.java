package com.mockify.backend.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException{
    public ForbiddenException(String message){
        super(message, HttpStatus.FORBIDDEN);
    }
}
