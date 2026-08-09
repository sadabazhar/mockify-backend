package com.mockify.backend.exception;

public class InvalidAssetException extends RuntimeException{
    public InvalidAssetException(String message){
        super(message);
    }
}
