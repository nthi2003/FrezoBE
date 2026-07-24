package com.frezo.product.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;


@Data
public class ProductCreateRequest {


    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên tối đa 200 ký tự")
    private String name;

    @NotBlank(message = "product.code.required")
    @Size(max = 30, message = "Mã tối đa 30 ký tự")
    private String code;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    private String imageUrl;

    @NotNull(message = "Giá sản phẩm không được để trống")
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
    private Boolean isActive;
    private String description;
}
