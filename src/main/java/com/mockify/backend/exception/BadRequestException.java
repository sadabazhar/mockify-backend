package com.mockify.backend.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException{

    public BadRequestException(String message){
        super(message, HttpStatus.BAD_REQUEST);
    }
}
