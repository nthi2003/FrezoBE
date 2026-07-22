package com.frezo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * @deprecated Từ v1.1, sử dụng {@link AppException} với {@link CommonErrorCode#UNAUTHORIZED} hoặc error code chuyên biệt.
 * <p>Migration:
 * <pre>
 * // Cũ
 * throw new AuthException("Invalid credentials");
 *
 * // Mới
 * throw new AppException(CommonErrorCode.UNAUTHORIZED);
 * throw new AppException(AuthErrorCode.INVALID_CREDENTIALS);
 * </pre>
 * Sẽ xoá ở v2.0.
 */
@Deprecated(since = "1.1", forRemoval = true)
@Getter
public class AuthException extends RuntimeException {

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
