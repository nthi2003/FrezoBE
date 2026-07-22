package com.frezo.product.service.impl.product;

import com.frezo.product.dto.response.ProductDashboardStats;
import com.frezo.product.repository.BatchRepository;
import com.frezo.product.repository.InventoryLogRepository;
import com.frezo.product.repository.MarketPriceRepository;
import com.frezo.product.repository.PriceConfigRepository;
import com.frezo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

/**
 * KPI + biểu đồ cho dashboard sản phẩm.
 * <p>Toàn bộ query đều được bọc try/catch — dashboard không được phép crash cả trang chỉ vì
 * 1 bảng thống kê thiếu dữ liệu.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductDashboardService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final PriceConfigRepository priceConfigRepository;
    private final MarketPriceRepository marketPriceRepository;

    public ProductDashboardStats getDashboardStats() {
        long total = safeCount(productRepository::countActiveProducts, "totalProducts");
        long lowStock = safeCount(productRepository::countLowStockProducts, "lowStockCount");
        long expiring = 0L;
        try {
            expiring = batchRepository.countExpiringSoon(LocalDate.now().plusDays(1));
        } catch (Exception e) {
            log.warn("[dashboard] countExpiringSoon failed: {}", e.getMessage());
        }
        return ProductDashboardStats.builder()
                .totalProducts(total)
                .lowStockCount(lowStock)
                .expiringSoonCount(expiring)
                .todayRevenue(0.0)
                .build();
    }

    public List<Map<String, Object>> getProfitChart(int days) {
        try {
            LocalDate fromDate = LocalDate.now().minusDays(days);
            List<Object[]> results = inventoryLogRepository.getProfitChartData(fromDate);
            return mapProfitRows(results);
        } catch (Exception e) {
            log.warn("[dashboard] getProfitChart failed — trả empty list. Lý do: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getPriceFluctuation() {
        try {
            List<Object[]> results = priceConfigRepository.getPriceFluctuation();
            return mapFluctuationRows(results);
        } catch (Exception e) {
            log.warn("[dashboard] getPriceFluctuation failed — trả empty list. Lý do: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    public List<Map<String, Object>> getMarketComparison() {
        try {
            List<Object[]> results = marketPriceRepository.getTodayMarketComparison();
            return mapMarketRows(results);
        } catch (Exception e) {
            log.warn("[dashboard] getMarketComparison failed — trả empty list. Lý do: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private long safeCount(LongSupplier supplier, String label) {
        try {
            return supplier.getAsLong();
        } catch (Exception e) {
            log.warn("[dashboard] {} failed: {}", label, e.getMessage());
            return 0L;
        }
    }

    private List<Map<String, Object>> mapProfitRows(List<Object[]> results) {
        List<Map<String, Object>> data = new ArrayList<>();
        if (results == null || results.isEmpty()) return data;
        for (Object[] res : results) {
            if (res == null || res.length < 3) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("name", res[0] == null ? "" : res[0].toString());
            map.put("cost", res[1] != null ? res[1] : 0.0);
            map.put("revenue", res[2] != null ? res[2] : 0.0);
            data.add(map);
        }
        return data;
    }

    private List<Map<String, Object>> mapFluctuationRows(List<Object[]> results) {
        List<Map<String, Object>> data = new ArrayList<>();
        if (results == null || results.isEmpty()) return data;
        for (Object[] res : results) {
            if (res == null || res.length < 4) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("name", res[0]);
            map.put("todayPrice", res[1]);
            map.put("yesterdayPrice", res[2]);
            double change = parsePercent(res[3]);
            map.put("change", change);
            map.put("status", change >= 0 ? "up" : "down");
            data.add(map);
        }
        return data;
    }

    private List<Map<String, Object>> mapMarketRows(List<Object[]> results) {
        List<Map<String, Object>> data = new ArrayList<>();
        if (results == null || results.isEmpty()) return data;
        for (Object[] res : results) {
            if (res == null || res.length < 5) continue;
            Map<String, Object> map = new HashMap<>();
            map.put("name", res[0]);
            map.put("marketName", res[1]);
            map.put("marketPrice", res[2]);
            map.put("unit", res[3]);
            map.put("yourPrice", res[4] != null ? res[4] : 0.0);
            data.add(map);
        }
        return data;
    }

    private double parsePercent(Object raw) {
        if (raw == null) return 0.0;
        try {
            return Double.parseDouble(raw.toString());
        } catch (NumberFormatException nfe) {
            log.warn("Failed to parse price change percentage: {}", raw);
            return 0.0;
        }
    }
}
