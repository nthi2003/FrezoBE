package com.frezo.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.frezo.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Wrapper response CHUẨN cho MỌI endpoint REST của Frezo (kể cả error).
 * <p>
 * Xem chi tiết: {@code FrezoBE/API_DESIGN_STANDARD.md §2 — Response Format}.
 * <p>
 * <b>Không dùng</b> {@code com.frezo.util.web.Response} (đã deprecated) và {@code Map<String,Object>} làm response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /** HTTP status code (200, 201, 400, 404...). */
    private Integer code;

    /** true khi 2xx, false khi ≥ 400. */
    private Boolean success;

    /** Message đã dịch i18n theo Accept-Language. */
    private String message;

    /** i18n key gốc — FE có thể dịch lại nếu cần. */
    private String messageCode;

    /** Payload: DTO hoặc {@link PageResponse}. */
    private T data;

    /** Tổng số record — chỉ dùng khi {@code data} là List không paginate. Nếu paginate: total nằm trong {@link PageResponse}. */
    private Long total;

    /** JWT token — chỉ dùng cho response login. */
    private String token;

    /** Correlation ID để debug — đọc từ MDC {@code traceId} (do TraceIdFilter set). */
    private String traceId;

    /** Server time UTC ISO-8601. */
    private OffsetDateTime timestamp;

    /** Request path — chỉ populate trong error response để trace lỗi. */
    private String path;

    /** Field validation errors — chỉ populate khi {@code messageCode = "validation.failed"}. */
    private Map<String, String> errors;

    // ----------------------------------------------------------------
    // Factory — SUCCESS
    // ----------------------------------------------------------------

    public static <T> ApiResponse<T> ok() {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .messageCode("success.default")
                .message("Thành công")
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .messageCode("success.default")
                .message("Thành công")
                .data(data)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String messageCode, String message) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .messageCode(messageCode)
                .message(message)
                .data(data)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.CREATED.value())
                .success(true)
                .messageCode("success.created")
                .message("Tạo mới thành công")
                .data(data)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    public static <T> ApiResponse<T> noContent() {
        return ApiResponse.<T>builder()
                .code(HttpStatus.NO_CONTENT.value())
                .success(true)
                .messageCode("success.default")
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    /** Login response — kèm JWT token. */
    public static <T> ApiResponse<T> loginSuccess(T data, String token) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .messageCode("success.login")
                .message("Đăng nhập thành công")
                .data(data)
                .token(token)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    // ----------------------------------------------------------------
    // Factory — ERROR
    // ----------------------------------------------------------------

    /** Error response từ ErrorCode enum (khuyến nghị). Message đã được resolve i18n bên ngoài. */
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String resolvedMessage, String path) {
        return ApiResponse.<T>builder()
                .code(errorCode.getStatus().value())
                .success(false)
                .messageCode(errorCode.getKey())
                .message(resolvedMessage)
                .path(path)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    /** Validation error — kèm field-level errors. */
    public static ApiResponse<Void> validationError(Map<String, String> errors, String resolvedMessage, String path) {
        return ApiResponse.<Void>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .messageCode("validation.failed")
                .message(resolvedMessage)
                .errors(errors)
                .path(path)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    // ----------------------------------------------------------------
    // Backward-compat factories — giữ để không break code cũ
    // ----------------------------------------------------------------

    /** @deprecated dùng {@link #ok(Object)}. */
    @Deprecated(since = "1.1", forRemoval = false)
    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    /** @deprecated dùng {@link #ok(Object, String, String)}. */
    @Deprecated(since = "1.1", forRemoval = false)
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message(message)
                .messageCode("success.default")
                .data(data)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    /** @deprecated dùng {@link #error(ErrorCode, String, String)} — cần trace path. */
    @Deprecated(since = "1.1", forRemoval = false)
    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(false)
                .message(message)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    /** @deprecated dùng {@link #error(ErrorCode, String, String)}. */
    @Deprecated(since = "1.1", forRemoval = false)
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .message(message)
                .traceId(currentTraceId())
                .timestamp(nowUtc())
                .build();
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private static String currentTraceId() {
        return MDC.get("traceId");
    }

    private static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }
}
