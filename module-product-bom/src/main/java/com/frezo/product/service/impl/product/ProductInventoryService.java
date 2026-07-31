package com.frezo.product.service.impl.product;

import com.frezo.product.dto.request.BatchImportRequest;
import com.frezo.product.entity.Batch;
import com.frezo.product.entity.InventoryLog;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.BatchRepository;
import com.frezo.product.repository.InventoryLogRepository;
import com.frezo.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Nhập kho nhanh theo chuyến xe — tạo batch + ghi inventory log.
 */
@Component
@RequiredArgsConstructor
public class ProductInventoryService {

    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final InventoryLogRepository inventoryLogRepository;

    @Transactional
    public void importBatch(BatchImportRequest request) {
        for (BatchImportRequest.BatchItem item : request.getItems()) {
            Product product = productRepository.findByCode(item.getProductCode())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + item.getProductCode()));

            Batch batch = Batch.builder()
                    .productId(product.getId())
                    .supplierId(request.getSupplierId())
                    .batchCode("BATCH-" + System.currentTimeMillis() + "-" + item.getProductCode())
                    .growingArea(item.getGrowingArea())
                    .importDate(LocalDate.now())
                    .expiryDate(item.getExpiryDate())
                    .initialQuantity(item.getQuantity())
                    .currentQuantity(item.getQuantity())
                    .costPrice(item.getCostPrice())
                    .build();
            batchRepository.save(batch);

            InventoryLog log = InventoryLog.builder()
                    .productId(product.getId())
                    .batchId(batch.getId())
                    .type("IMPORT")
                    .quantity(item.getQuantity())
                    .note("Nhập kho nhanh từ chuyến xe")
                    .build();
            inventoryLogRepository.save(log);
        }
    }

    /**
     * Lịch sử giá vốn theo lô nhập NCC (product_batches).
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCostHistory(String productId) {
        if (productId == null || productId.isBlank()) {
            return List.of();
        }
        List<Batch> batches = batchRepository.findByProductIdAndIsDeletedFalseOrderByImportDateAsc(productId);
        List<Map<String, Object>> points = new ArrayList<>();
        for (Batch b : batches) {
            if (b.getCostPrice() == null) continue;
            LocalDateTime date = b.getImportDate() != null
                    ? LocalDateTime.of(b.getImportDate(), LocalTime.MIDDAY)
                    : b.getCreatedDate();
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", date);
            point.put("unitCost", b.getCostPrice());
            point.put("qty", b.getInitialQuantity());
            point.put("batchCode", b.getBatchCode());
            point.put("supplierId", b.getSupplierId());
            point.put("source", "BATCH");
            points.add(point);
        }
        return points;
    }
}
