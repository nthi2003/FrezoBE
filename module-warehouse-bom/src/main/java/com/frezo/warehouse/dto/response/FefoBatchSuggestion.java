package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FefoBatchSuggestion {
    private String batchId;
    private String batchCode;
    private String expiryDate;
    private Double qtyAvailable;
    private Double suggestedQty;
    private Integer daysToExpiry;
    private String expiryWarning;
    private String supplierName;
}
