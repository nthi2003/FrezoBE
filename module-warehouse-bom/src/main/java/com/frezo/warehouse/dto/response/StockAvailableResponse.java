package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tồn khả dụng theo sản phẩm + kho (+ lô tùy chọn).
 * {@code quantityAvailable} = onHand − reserved (đã cộng dồn các dòng tồn nếu không chỉ định lô).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAvailableResponse {
    private String productId;
    private String productCode;
    private String productName;
    private String warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String batchId;
    private Double quantityOnHand;
    private Double quantityReserved;
    private Double quantityAvailable;
}
