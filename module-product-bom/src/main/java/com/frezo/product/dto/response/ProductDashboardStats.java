package com.frezo.product.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProductDashboardStats {
    private long totalProducts;
    private long lowStockCount;
    private long expiringSoonCount;
    private Double todayRevenue;
}
