package com.frezo.product.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PriceUpdateRequest {
    private String productCode;
    private List<UnitPrice> unitPrices;

    @Data
    public static class UnitPrice {
        private String unitName;
        private String priceGroupCode;
        private Double newPrice;
    }
}
