package com.frezo.common.exception;

import lombok.Getter;

import java.io.Serial;

/**
 * @deprecated Từ v1.1, sử dụng {@link AppException} với {@code QthtErrorCode} enum thay vì lớp exception riêng cho từng module.
 * <p>Migration:
 * <pre>
 * // Cũ
 * throw new QTHTException("invalid.department.entity.not.found", id);
 *
 * // Mới
 * throw new AppException(QthtErrorCode.DEPARTMENT_NOT_FOUND, id);
 * </pre>
 * Class này giữ lại để backward-compat, sẽ xoá ở v2.0.
 */
@Deprecated(since = "1.1", forRemoval = true)
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

    public QTHTException(String messageKey, Object... args) {
        super(messageKey);
        this.messageKey = messageKey;
        this.args = args;
    }

    @Override
    public String getMessage() {
        return messageKey;
    }
}
