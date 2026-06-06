package com.frezo.util.web;

import lombok.Builder;
import lombok.Data;
import org.apache.http.HttpStatus;
import org.springframework.data.domain.Page;

import jakarta.validation.constraints.NotNull;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;


@Data
@Builder
public class Response<T> {
    @NotNull
    private boolean success;
    @NotNull
    private int statusCode;
    private String message;
    private String messageCode;
    private String debugMessage;
    @NotNull
    private T data;
    @NotNull
    private long total;

    public Response<T> setMessage(String messageCode, String message) {
        this.message = message;
        this.messageCode = messageCode;
        return this;
    }

    public Response<T> setMessage(String messageCode) {
        this.message = "";
        this.messageCode = messageCode;
        return this;
    }

    public static <T> Response<T> ok(T data) {
        long size = 1L;
        if (data != null && data.getClass().isArray()) {
            size = Array.getLength(data);
        } else if (data instanceof Collection) {
            size = ((Collection<?>) data).size();
        }
        return Response.<T>builder()
            .success(true)
            .statusCode(HttpStatus.SC_OK)
            .data(data)
            .message("OK")
            .messageCode("ok")
            .total(size)
            .debugMessage("")
            .build();
    }

    public static <T> Response<T> ok() {
        return Response.<T>builder()
            .success(true)
            .statusCode(HttpStatus.SC_OK)
            .message("OK")
            .messageCode("ok")
            .total(1L)
            .debugMessage("")
            .build();
    }

    public static <T> Response<List<T>> ok(Page<T> data) {
        return Response.<List<T>>builder()
            .success(true)
            .statusCode(HttpStatus.SC_OK)
            .data(data.getContent())
            .message("OK")
            .messageCode("ok")
            .total((Long) data.getTotalElements())
            .debugMessage("")
            .build();
    }

    public static <T> Response<T> created(T data) {
        return Response.<T>builder()
            .success(true)
            .statusCode(HttpStatus.SC_CREATED)
            .data(data)
            .message("CREATED")
            .messageCode("created")
            .total(1L)
            .debugMessage("")
            .build();
    }

    public static <T> Response<T> deleted() {
        return Response.<T>builder()
            .success(true)
            .statusCode(HttpStatus.SC_NO_CONTENT)
            .message("DELETED")
            .messageCode("deleted")
            .total(1L)
            .debugMessage("")
            .build();
    }

    public static <T> Response<T> notfound() {
        return Response.<T>builder()
            .success(false)
            .statusCode(HttpStatus.SC_NOT_FOUND)
            .message("Dữ liệu không tồn tại")
            .messageCode("not-found")
            .total(1L)
            .debugMessage("")
            .build();
    }

    public static <T> Response<T> bad() {
        return Response.<T>builder()
            .success(false)
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .message("")
            .messageCode("")
            .total(1L)
            .debugMessage("")
            .build();
    }

    public static <T> Response<T> error(String message) {
        return Response.<T>builder()
            .success(false)
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .message(message)
            .messageCode("error")
            .total(0L)
            .debugMessage("")
            .build();
    }

    public static <T> Response<T> error(String message, String messageCode) {
        return Response.<T>builder()
            .success(false)
            .statusCode(HttpStatus.SC_BAD_REQUEST)
            .message(message)
            .messageCode(messageCode)
            .total(0L)
            .debugMessage("")
            .build();
    }

    public static <T> Response<T> error(int statusCode, String message, String messageCode) {
        return Response.<T>builder()
            .success(false)
            .statusCode(statusCode)
            .message(message)
            .messageCode(messageCode)
            .total(0L)
            .debugMessage("")
            .build();
    }
}
