package com.frezo.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProductSalePriceUpdateRequest {

    @NotNull
    @Min(0)
    private Double price;

    /** Ghi chú lý do đổi giá (tuỳ chọn). */
    private String note;
}
