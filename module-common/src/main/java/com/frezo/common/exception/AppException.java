package com.frezo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final Object[] args;

    public AppException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.args = null;
    }

    public AppException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.args = null;
    }

    public AppException(String message, Object... args) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.args = args;
    }

    public AppException(String message, HttpStatus status, Object... args) {
        super(message);
        this.status = status;
        this.args = args;
    }
}
