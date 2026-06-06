package com.frezo.common.exception;

import com.frezo.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import java.sql.SQLException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex, WebRequest request, Locale locale) {
        log.error("AppException: {}", ex.getMessage());
        String message = ex.getMessage();
        try {
            message = messageSource.getMessage(ex.getMessage(), ex.getArgs(), locale);
        } catch (Exception e) {
            log.warn("Message key not found: {}", ex.getMessage());
        }
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getStatus().value(), message));
    }

    @ExceptionHandler(QTHTException.class)
    public ResponseEntity<ApiResponse<Void>> handleQTHTException(QTHTException ex, WebRequest request, Locale locale) {
        log.error("QTHTException: {}", ex.getMessage());
        String message = ex.getMessage();
        try {
            message = messageSource.getMessage(ex.getMessage(), ex.getArgs(), locale);
        } catch (Exception e) {
            log.warn("Message key not found: {}", ex.getMessage());
        }
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if ("error.access.denied".equals(ex.getMessage())) {
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status.value(), message));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException ex, WebRequest request, Locale locale) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ApiResponse<Map<String, String>> response = ApiResponse.<Map<String, String>>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .message("Validation Failed")
                .data(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({SQLException.class, DataIntegrityViolationException.class, JpaSystemException.class})
    public ResponseEntity<ApiResponse<Void>> handleSqlException(Exception ex, WebRequest request) {
        log.error("SQL Error: ", ex); 
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(HttpStatus.BAD_REQUEST.value(), "Lỗi truy vấn dữ liệu (SQL Error)"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected Error: ", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), ex.getMessage() != null ? ex.getMessage() : "Lỗi hệ thống (Internal Server Error)"));
    }
                        
}
