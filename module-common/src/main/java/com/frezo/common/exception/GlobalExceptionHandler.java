package com.frezo.common.exception;

import com.frezo.common.response.ApiResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Global exception handler cho toàn hệ thống Frezo.
 * <p>
 * Xem chi tiết: {@code FrezoBE/SPRING_BOOT_BEST_PRACTICE.md §6 — Exception Handling}
 * và {@code FrezoBE/API_DESIGN_STANDARD.md §5}.
 * <p>
 * Response luôn là {@link ApiResponse} với đầy đủ {@code code, success, messageCode, message, path, traceId, timestamp}.
 * Validation error kèm map {@code errors} field → message.
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    // ================================================================
    // BUSINESS — AppException (khuyến nghị)
    // ================================================================

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleApp(
            AppException ex, HttpServletRequest request, Locale locale) {
        String messageKey = ex.messageKey();
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST;
        String resolved = resolveMessage(messageKey, ex.getArgs(), locale,
                ex.getErrorCode() != null ? ex.getErrorCode().getDefaultMessage() : messageKey);

        // WARN cho 4xx client error, ERROR cho 5xx server error
        if (status.is5xxServerError()) {
            log.error("AppException [{}] at {}: {}", messageKey, request.getRequestURI(), resolved, ex);
        } else {
            log.warn("AppException [{}] at {}: {}", messageKey, request.getRequestURI(), resolved);
        }

        ApiResponse<Void> body = ex.getErrorCode() != null
                ? ApiResponse.error(ex.getErrorCode(), resolved, request.getRequestURI())
                : buildErrorBody(status.value(), messageKey, resolved, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    // ================================================================
    // BUSINESS — legacy exceptions (deprecated, giữ backward-compat)
    // ================================================================

    @SuppressWarnings("removal")
    @ExceptionHandler(QTHTException.class)
    public ResponseEntity<ApiResponse<Void>> handleLegacyQtht(
            QTHTException ex, HttpServletRequest request, Locale locale) {
        String messageKey = ex.getMessage();
        HttpStatus status = "error.access.denied".equals(messageKey) ? HttpStatus.FORBIDDEN : HttpStatus.BAD_REQUEST;
        String resolved = resolveMessage(messageKey, ex.getArgs(), locale, messageKey);
        log.warn("[LEGACY] QTHTException [{}] at {}: {} — migrate to AppException + ErrorCode enum",
                messageKey, request.getRequestURI(), resolved);
        return ResponseEntity.status(status).body(
                buildErrorBody(status.value(), messageKey, resolved, request.getRequestURI()));
    }

    @SuppressWarnings("removal")
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleLegacyAuth(
            AuthException ex, HttpServletRequest request, Locale locale) {
        HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.UNAUTHORIZED;
        String resolved = resolveMessage(ex.getMessage(), null, locale, ex.getMessage());
        log.warn("[LEGACY] AuthException at {}: {} — migrate to AppException + CommonErrorCode.UNAUTHORIZED",
                request.getRequestURI(), resolved);
        return ResponseEntity.status(status).body(
                buildErrorBody(status.value(), CommonErrorCode.UNAUTHORIZED.getKey(), resolved, request.getRequestURI()));
    }

    // ================================================================
    // VALIDATION — @Valid trong @RequestBody
    // ================================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleBeanValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request, Locale locale) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        String resolved = resolveMessage(CommonErrorCode.VALIDATION_FAILED.getKey(), null, locale,
                CommonErrorCode.VALIDATION_FAILED.getDefaultMessage());
        log.warn("Validation failed at {}: {}", request.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(
                ApiResponse.validationError(errors, resolved, request.getRequestURI()));
    }

    /** {@code @Validated} trên query/path variable → {@link ConstraintViolationException}. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request, Locale locale) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(cv ->
                errors.putIfAbsent(cv.getPropertyPath().toString(), cv.getMessage()));
        String resolved = resolveMessage(CommonErrorCode.VALIDATION_FAILED.getKey(), null, locale,
                CommonErrorCode.VALIDATION_FAILED.getDefaultMessage());
        log.warn("Constraint violation at {}: {}", request.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(
                ApiResponse.validationError(errors, resolved, request.getRequestURI()));
    }

    // ================================================================
    // REQUEST FORMAT — 400
    // ================================================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(
            HttpMessageNotReadableException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.REQUEST_BODY_INVALID.getKey(), null, locale,
                CommonErrorCode.REQUEST_BODY_INVALID.getDefaultMessage());
        log.warn("Unreadable request body at {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.error(CommonErrorCode.REQUEST_BODY_INVALID, resolved, request.getRequestURI()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.REQUEST_PARAM_MISSING.getKey(),
                new Object[]{ex.getParameterName()}, locale,
                CommonErrorCode.REQUEST_PARAM_MISSING.getDefaultMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.error(CommonErrorCode.REQUEST_PARAM_MISSING, resolved, request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.REQUEST_PARAM_TYPE_INVALID.getKey(),
                new Object[]{ex.getName(), String.valueOf(ex.getValue())}, locale,
                CommonErrorCode.REQUEST_PARAM_TYPE_INVALID.getDefaultMessage());
        return ResponseEntity.badRequest().body(
                ApiResponse.error(CommonErrorCode.REQUEST_PARAM_TYPE_INVALID, resolved, request.getRequestURI()));
    }

    // ================================================================
    // SECURITY — 401 / 403
    // ================================================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(
            AuthenticationException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.UNAUTHORIZED.getKey(), null, locale,
                CommonErrorCode.UNAUTHORIZED.getDefaultMessage());
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ApiResponse.error(CommonErrorCode.UNAUTHORIZED, resolved, request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.FORBIDDEN.getKey(), null, locale,
                CommonErrorCode.FORBIDDEN.getDefaultMessage());
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.error(CommonErrorCode.FORBIDDEN, resolved, request.getRequestURI()));
    }

    // ================================================================
    // ROUTING / METHOD — 404 / 405
    // ================================================================

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.PATH_NOT_FOUND.getKey(),
                new Object[]{request.getRequestURI()}, locale,
                CommonErrorCode.PATH_NOT_FOUND.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.error(CommonErrorCode.PATH_NOT_FOUND, resolved, request.getRequestURI()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.METHOD_NOT_ALLOWED.getKey(),
                new Object[]{ex.getMethod()}, locale,
                CommonErrorCode.METHOD_NOT_ALLOWED.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED, resolved, request.getRequestURI()));
    }

    // ================================================================
    // DATA — 409
    // ================================================================

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, OptimisticLockException.class})
    public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(
            Exception ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.CONCURRENT_MODIFICATION.getKey(), null, locale,
                CommonErrorCode.CONCURRENT_MODIFICATION.getDefaultMessage());
        log.warn("Optimistic lock conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(CommonErrorCode.CONCURRENT_MODIFICATION, resolved, request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.DATA_INTEGRITY_VIOLATION.getKey(), null, locale,
                CommonErrorCode.DATA_INTEGRITY_VIOLATION.getDefaultMessage());
        log.error("Data integrity violation at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                ApiResponse.error(CommonErrorCode.DATA_INTEGRITY_VIOLATION, resolved, request.getRequestURI()));
    }

    @ExceptionHandler({SQLException.class, JpaSystemException.class})
    public ResponseEntity<ApiResponse<Void>> handleSqlException(
            Exception ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.SQL_ERROR.getKey(), null, locale,
                CommonErrorCode.SQL_ERROR.getDefaultMessage());
        log.error("SQL error at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(CommonErrorCode.SQL_ERROR, resolved, request.getRequestURI()));
    }

    // ================================================================
    // UPLOAD — 413
    // ================================================================

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.UPLOAD_SIZE_EXCEEDED.getKey(), null, locale,
                CommonErrorCode.UPLOAD_SIZE_EXCEEDED.getDefaultMessage());
        log.warn("Upload size exceeded at {}: {} bytes max", request.getRequestURI(), ex.getMaxUploadSize());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                ApiResponse.error(CommonErrorCode.UPLOAD_SIZE_EXCEEDED, resolved, request.getRequestURI()));
    }

    // ================================================================
    // FALLBACK — 500
    // ================================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(
            Exception ex, HttpServletRequest request, Locale locale) {
        String resolved = resolveMessage(CommonErrorCode.INTERNAL_ERROR.getKey(), null, locale,
                CommonErrorCode.INTERNAL_ERROR.getDefaultMessage());
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.error(CommonErrorCode.INTERNAL_ERROR, resolved, request.getRequestURI()));
    }

    // ================================================================
    // Helpers
    // ================================================================

    private String resolveMessage(String key, Object[] args, Locale locale, String fallback) {
        if (key == null) return fallback;
        try {
            return messageSource.getMessage(key, args, locale);
        } catch (NoSuchMessageException nsm) {
            log.warn("Missing i18n key: {} (fallback: {})", key, fallback);
            return fallback;
        }
    }

    private ApiResponse<Void> buildErrorBody(int code, String messageCode, String resolved, String path) {
        return ApiResponse.<Void>builder()
                .code(code)
                .success(false)
                .messageCode(messageCode)
                .message(resolved)
                .path(path)
                .traceId(org.slf4j.MDC.get("traceId"))
                .timestamp(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC))
                .build();
    }
}
