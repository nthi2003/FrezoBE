package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Điểm biến động giá nhập NCC theo thời gian (từ dòng phiếu nhập kho).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPriceHistoryPoint {
    /** Thời điểm nhập / tạo phiếu */
    private LocalDateTime date;
    /** Đơn giá nhập (VNĐ) */
    private Double unitCost;
    /** Số lượng nhận (nếu có) */
    private Double qty;
    private String grnId;
    private String grnCode;
    private String supplierId;
    private String supplierName;
    private String status;
    /** Nguồn dữ liệu: GRN */
    @Builder.Default
    private String source = "GRN";
}
