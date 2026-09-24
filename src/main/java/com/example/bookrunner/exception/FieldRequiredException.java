package com.example.bookrunner.exception;

public class FieldRequiredException extends RuntimeException{
    public FieldRequiredException(String message) {
        super(message);
    }
}
