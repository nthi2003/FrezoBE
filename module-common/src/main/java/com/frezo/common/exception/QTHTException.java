package com.frezo.common.exception;

import lombok.Getter;

import java.io.Serial;


public class QTHTException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;
    private String messageKey;
    @Getter
    private Object[] args;

    public QTHTException(String messageKey) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = null;
    }

    @Override
    public String getMessage() {
        return messageKey;
    }
}
