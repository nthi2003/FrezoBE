package com.frezo.common.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditAction {
    String value() default ""; // Mô tả hành động (e.g., "Thêm mới sản phẩm")
    String entity() default ""; // Tên bảng/entity
    String action() default "VIEW"; // CREATE, UPDATE, DELETE, VIEW
}
