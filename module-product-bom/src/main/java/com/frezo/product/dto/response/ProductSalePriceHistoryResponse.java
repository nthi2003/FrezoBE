package com.frezo.product.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSalePriceHistoryResponse {

    private String id;
    private String productId;
    private Double oldPrice;
    private Double newPrice;
    private String note;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
