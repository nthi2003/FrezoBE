package com.frezo.product.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Payload tạo sản phẩm mới.
 * Chỉ `name`, `price`, `category` là bắt buộc — các field còn lại tuỳ chọn để form
 * "Thêm nhanh" không bị chặn. Ảnh có thể bổ sung sau khi upload xong.
 */
@Data
public class ProductCreateRequest {

    // ===== Basic =====
    @NotBlank(message = "Tên sản phẩm không được để trống")
    @Size(max = 200, message = "Tên tối đa 200 ký tự")
    private String name;

    @Size(max = 30, message = "Mã tối đa 30 ký tự")
    private String code; // nếu để trống, service sẽ auto-generate (VD SP001, SP002...)

    @NotBlank(message = "Danh mục không được để trống")
    private String category;

    private String imageUrl; // optional — có thể upload sau

    // ===== Pricing =====
    @NotNull(message = "Giá sản phẩm không được để trống")
    @PositiveOrZero(message = "Giá phải ≥ 0")
    private Double price;

    // ===== Origin & Season (nông sản) =====
    @Size(max = 100, message = "Nguồn gốc tối đa 100 ký tự")
    private String origin;

    @Size(max = 100, message = "Mùa vụ tối đa 100 ký tự")
    private String season;

    // ===== Stock alerts =====
    @PositiveOrZero(message = "Ngưỡng cảnh báo phải ≥ 0")
    private Double warningThreshold;

    @PositiveOrZero(message = "Số ngày cảnh báo phải ≥ 0")
    private Integer expiryAlertDays;

    // ===== Marketing =====
    private Double rating;
    private Boolean isNew;
    private Boolean isActive;
    private String description;
}
