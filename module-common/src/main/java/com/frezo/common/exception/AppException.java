package com.frezo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Business exception thống nhất cho toàn hệ thống Frezo.
 * <p>
 * <b>Cách dùng khuyến nghị (mới)</b> — truyền {@link ErrorCode} enum thay vì magic string:
 * <pre>
 * throw new AppException(QthtErrorCode.DEPARTMENT_NOT_FOUND, departmentId);
 * throw new AppException(CommonErrorCode.CONFLICT);
 * throw new AppException(QthtErrorCode.DEPARTMENT_CODE_EXISTS, cause, code);
 * </pre>
 * <p>
 * <b>Constructor cũ (deprecated nhưng vẫn hoạt động)</b> — nhận i18n key trực tiếp.
 * Sẽ được migrate dần sang constructor mới.
 * <p>
 * Được xử lý bởi {@link GlobalExceptionHandler}:
 * <ul>
 *   <li>Lookup i18n message qua {@link #getErrorCode()}.{@code key()} nếu có, ngược lại dùng {@link #getMessage()}.</li>
 *   <li>Trả HTTP status theo {@link #getStatus()}.</li>
 *   <li>Format response {@code ApiResponse&lt;Void&gt;} với {@code messageCode}, {@code message}, {@code traceId}, {@code path}.</li>
 * </ul>
 */
@Getter
public class AppException extends RuntimeException {

    /** Có thể null nếu dùng constructor cũ (String message). Ưu tiên dùng constructor có ErrorCode. */
    private final ErrorCode errorCode;

    private final HttpStatus status;

    private final Object[] args;

    // ------------------------------------------------------------
    // Constructors MỚI — dùng ErrorCode enum (khuyến nghị)
    // ------------------------------------------------------------

    public AppException(ErrorCode errorCode, Object... args) {
        super(errorCode.getKey());
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
        this.args = args;
    }

    public AppException(ErrorCode errorCode, Throwable cause, Object... args) {
        super(errorCode.getKey(), cause);
        this.errorCode = errorCode;
        this.status = errorCode.getStatus();
        this.args = args;
    }

    // ------------------------------------------------------------
    // Constructors CŨ — giữ để backward-compat, sẽ migrate dần
    // ------------------------------------------------------------

    /** @deprecated dùng {@link #AppException(ErrorCode, Object...)} với enum ErrorCode thay vì magic string key. */
    @Deprecated(since = "1.1", forRemoval = false)
    public AppException(String message) {
        super(message);
        this.errorCode = null;
        this.status = HttpStatus.BAD_REQUEST;
        this.args = null;
    }

    /** @deprecated dùng {@link #AppException(ErrorCode, Object...)}. */
    @Deprecated(since = "1.1", forRemoval = false)
    public AppException(String message, HttpStatus status) {
        super(message);
        this.errorCode = null;
        this.status = status;
        this.args = null;
    }

    /** @deprecated dùng {@link #AppException(ErrorCode, Object...)}. */
    @Deprecated(since = "1.1", forRemoval = false)
    public AppException(String message, Object... args) {
        super(message);
        this.errorCode = null;
        this.status = HttpStatus.BAD_REQUEST;
        this.args = args;
    }

    /** @deprecated dùng {@link #AppException(ErrorCode, Object...)}. */
    @Deprecated(since = "1.1", forRemoval = false)
    public AppException(String message, HttpStatus status, Object... args) {
        super(message);
        this.errorCode = null;
        this.status = status;
        this.args = args;
    }

    /**
     * Trả về i18n key hiệu quả để lookup MessageSource.
     * Ưu tiên {@link ErrorCode#getKey()} nếu có, ngược lại {@link #getMessage()} (backward-compat).
     */
    public String messageKey() {
        return errorCode != null ? errorCode.getKey() : getMessage();
    }

    /**
     * Trả về messageCode (i18n key) để đưa vào response — FE có thể dịch lại nếu cần.
     */
    public String messageCode() {
        return messageKey();
    }
}
