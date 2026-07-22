package com.frezo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Contract cho error code enum của mỗi module.
 * Mỗi module tạo enum implement interface này (VD: {@code QthtErrorCode}, {@code CustomerErrorCode}...)
 * <p>
 * Method names dùng prefix {@code get*} để tương thích Lombok {@code @Getter} — mỗi enum chỉ cần
 * khai báo field {@code key / status / defaultMessage} và Lombok sinh getter tự động.
 * <p>
 * Ví dụ:
 * <pre>
 * &#64;Getter
 * &#64;RequiredArgsConstructor
 * public enum QthtErrorCode implements ErrorCode {
 *     DEPARTMENT_NOT_FOUND("invalid.department.entity.not.found", HttpStatus.NOT_FOUND, "Phòng ban không tồn tại"),
 *     DEPARTMENT_CODE_EXISTS("department.code.already.exists", HttpStatus.CONFLICT, "Mã phòng ban đã tồn tại");
 *
 *     private final String key;
 *     private final HttpStatus status;
 *     private final String defaultMessage;
 * }
 * </pre>
 */
public interface ErrorCode {

    /** i18n message key trong {@code messages*.properties}. */
    String getKey();

    /** HTTP status trả về client. */
    HttpStatus getStatus();

    /** Message fallback khi không tìm thấy i18n key. */
    String getDefaultMessage();
}
