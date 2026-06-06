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
        super(messageKey); // Gọi constructor của Exception với message
        this.messageKey = messageKey;
        this.args = null;
    }
    public QTHTException(String messageKey, Object... args) {
        super(messageKey); // Gọi constructor của Exception với message
        this.messageKey = messageKey;
        this.args = args;
    }
    @Override
    public String getMessage() {
        return messageKey;
    }

}
