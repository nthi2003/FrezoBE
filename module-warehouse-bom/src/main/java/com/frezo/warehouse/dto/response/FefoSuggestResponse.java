package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FefoSuggestResponse {
    private String productId;
    private String warehouseId;
    private Double requestedQty;
    private Double allocatedQty;
    private List<FefoBatchSuggestion> suggestions;
}
