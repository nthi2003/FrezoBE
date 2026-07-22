package com.frezo.product.dto.request;

import lombok.Data;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Payload cập nhật sản phẩm — mọi field đều optional (partial update).
 * Mapper dùng `NullValuePropertyMappingStrategy.IGNORE` để chỉ apply field non-null.
 */
@Data
public class ProductUpdateRequest {
    @Size(max = 200, message = "Tên tối đa 200 ký tự")
    private String name;

    @Size(max = 30, message = "Mã tối đa 30 ký tự")
    private String code;

    private String category;

    private String imageUrl;

    @PositiveOrZero(message = "Giá phải ≥ 0")
    private Double price;

    @Size(max = 100, message = "Nguồn gốc tối đa 100 ký tự")
    private String origin;

    @Size(max = 100, message = "Mùa vụ tối đa 100 ký tự")
    private String season;

    @PositiveOrZero(message = "Ngưỡng cảnh báo phải ≥ 0")
    private Double warningThreshold;

    @PositiveOrZero(message = "Số ngày cảnh báo phải ≥ 0")
    private Integer expiryAlertDays;

    private Double rating;
    private Boolean isNew;
    private String description;
    private Boolean isActive;
}
