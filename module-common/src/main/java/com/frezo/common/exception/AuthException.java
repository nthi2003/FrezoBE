package com.frezo.common.exception;

import org.springframework.http.HttpStatus;

public class AuthException extends RuntimeException{
    private final HttpStatus status;
    public AuthException(String message) {
        super(message);
        this.status = HttpStatus.UNAUTHORIZED;
    }
    public AuthException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
