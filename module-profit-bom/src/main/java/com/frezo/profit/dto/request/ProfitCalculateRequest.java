package com.frezo.profit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProfitCalculateRequest {

    @NotBlank
    private String productId;

    @NotNull
    @Positive
    private Double quantity;

    /** Giá bán/đơn vị — nếu null dùng giá catalog sản phẩm */
    private Double salePrice;

    /** Số tháng phân bổ chi phí cố định (MVP: 1 tháng) */
    private Integer allocationMonths;
}
