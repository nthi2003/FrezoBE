package com.frezo.crm.dto;

import com.frezo.crm.common.InvoiceStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class InvoiceRequest {
    @NotBlank
    private String customerId;
    private String customerName;
    private String quoteId;
    private LocalDate issuedDate;
    private LocalDate dueDate;
    private String currency;
    private InvoiceStatus status;
    private String notes;
    /** Sale phụ trách — nếu trống sẽ lấy từ Deal.owner qua Quote. */
    private String salespersonUsername;
    /** Override % hoa hồng cho đơn này (null = dùng cấu hình sale / mặc định). */
    private BigDecimal commissionRatePercent;
    private List<Item> items;

    @Data
    public static class Item {
        private String productId;
        private String productName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
    }
}
