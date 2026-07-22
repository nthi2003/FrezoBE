package com.frezo.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error code dùng chung cho toàn hệ thống — các trường hợp không thuộc business domain cụ thể.
 * <p>
 * Module business (qtht, customer, warehouse...) tạo enum riêng ({@code QthtErrorCode}...) cho error thuộc domain đó.
 * <p>
 * i18n key tương ứng phải tồn tại trong {@code module-server/src/main/resources/i18n/messages*.properties}.
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 400 — client input errors
    INVALID_REQUEST("error.invalid.request", HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ"),
    VALIDATION_FAILED("validation.failed", HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    REQUEST_BODY_INVALID("error.request.body.invalid", HttpStatus.BAD_REQUEST, "Nội dung request không đọc được"),
    REQUEST_PARAM_MISSING("error.request.param.missing", HttpStatus.BAD_REQUEST, "Thiếu tham số bắt buộc"),
    REQUEST_PARAM_TYPE_INVALID("error.request.param.type.invalid", HttpStatus.BAD_REQUEST, "Kiểu tham số không hợp lệ"),
    INVALID_SORT_FIELD("error.invalid.sort.field", HttpStatus.BAD_REQUEST, "Trường sắp xếp không hợp lệ"),

    // 401 / 403 — auth
    UNAUTHORIZED("error.unauthorized", HttpStatus.UNAUTHORIZED, "Chưa đăng nhập hoặc phiên đã hết hạn"),
    FORBIDDEN("error.access.denied", HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này"),

    // 404 / 405 / 410
    NOT_FOUND("error.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên"),
    PATH_NOT_FOUND("error.request.path.not.found", HttpStatus.NOT_FOUND, "Đường dẫn không tồn tại"),
    METHOD_NOT_ALLOWED("error.request.method.not.allowed", HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ"),
    GONE("error.gone", HttpStatus.GONE, "API đã ngừng hỗ trợ"),

    // 409
    CONFLICT("error.conflict", HttpStatus.CONFLICT, "Xung đột dữ liệu"),
    CONCURRENT_MODIFICATION("error.concurrent.modification", HttpStatus.CONFLICT, "Có người khác vừa cập nhật, vui lòng tải lại"),
    DATA_INTEGRITY_VIOLATION("error.data.integrity.violation", HttpStatus.CONFLICT, "Vi phạm ràng buộc dữ liệu"),
    IDEMPOTENCY_KEY_CONFLICT("error.idempotency.key.conflict", HttpStatus.CONFLICT, "Idempotency key đã dùng với payload khác"),

    // 413 / 415
    UPLOAD_SIZE_EXCEEDED("error.upload.size.exceeded", HttpStatus.PAYLOAD_TOO_LARGE, "Tệp tải lên vượt quá kích thước cho phép"),
    UNSUPPORTED_MEDIA_TYPE("error.unsupported.media.type", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng tệp không được hỗ trợ"),

    // 429
    RATE_LIMIT_EXCEEDED("error.rate.limit", HttpStatus.TOO_MANY_REQUESTS, "Quá nhiều yêu cầu, vui lòng thử lại sau"),

    // 500
    INTERNAL_ERROR("error.internal", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống, vui lòng thử lại sau"),
    SQL_ERROR("error.sql", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi truy vấn dữ liệu"),

    // 502 / 503 / 504
    EXTERNAL_SERVICE_ERROR("error.external.service", HttpStatus.BAD_GATEWAY, "Lỗi dịch vụ bên ngoài"),
    SERVICE_UNAVAILABLE("error.service.unavailable", HttpStatus.SERVICE_UNAVAILABLE, "Dịch vụ tạm thời không khả dụng"),
    UPSTREAM_TIMEOUT("error.upstream.timeout", HttpStatus.GATEWAY_TIMEOUT, "Dịch vụ bên ngoài không phản hồi");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
