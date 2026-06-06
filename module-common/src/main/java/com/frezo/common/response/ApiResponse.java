package com.frezo.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Integer code;
    private Boolean success;
    private String message;
    private String messageCode;
    private T data;
    private Long total;
    private String token;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message("Thành công")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> loginSuccess(T data, String token) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.OK.value())
                .success(true)
                .message("Đăng nhập thành công")
                .data(data)
                .token(token)
                .build();
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return ApiResponse.<T>builder()
                .code(code)
                .success(false)
                .message(message)
                .build();
    }


    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .code(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .message(message)
                .build();
    }
}
