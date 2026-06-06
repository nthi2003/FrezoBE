package com.frezo.product.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class ProductCreateRequest {
    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    @NotNull(message = "Giá sản phẩm không được để trống")
    private Double price;

    @NotBlank(message = "URL hình ảnh không được để trống")
    private String imageUrl;

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    private Double rating;
    private Boolean isNew;
    private String description;
}
