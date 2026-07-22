package com.frezo.crm.dto;

import com.frezo.crm.common.QuoteStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class QuoteRequest {
    private String dealId;
    @NotBlank
    private String customerId;
    private LocalDate issuedDate;
    private LocalDate validUntil;
    private String currency;
    private QuoteStatus status;
    private String notes;
    private List<Item> items;

    @Data
    public static class Item {
        private String productId;
        private String productName;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
        private BigDecimal discountPct;
        private String description;
    }
}
