package com.frezo.warehouse.service.impl;

import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.entity.StockAlert;
import com.frezo.warehouse.entity.StockBatch;
import com.frezo.warehouse.repository.StockAlertRepository;
import com.frezo.warehouse.repository.StockBatchRepository;
import com.frezo.warehouse.service.ExpiryAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpiryAlertServiceImpl implements ExpiryAlertService {

    private static final int DEFAULT_ALERT_LEAFY = 1;
    private static final int DEFAULT_ALERT_ROOT = 3;

    private final StockBatchRepository batchRepository;
    private final StockAlertRepository alertRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public int scanAndRaiseExpiryAlerts() {
        List<StockBatch> batches = batchRepository.findAll().stream()
                .filter(b -> Boolean.FALSE.equals(b.getIsDeleted()))
                .filter(b -> "ACTIVE".equals(b.getStatus()))
                .filter(b -> b.getQtyOnHand() != null && b.getQtyOnHand() > 0)
                .filter(b -> b.getExpiryDate() != null)
                .toList();

        int raised = 0;
        LocalDate today = LocalDate.now();
        for (StockBatch batch : batches) {
            int daysLeft = (int) ChronoUnit.DAYS.between(today, batch.getExpiryDate());
            int alertDays = resolveExpiryAlertDays(batch.getProductId());
            if (daysLeft > alertDays) continue;

            String idem = "expiry|" + batch.getId() + "|" + today;
            if (alertRepository.findByIdempotencyKeyAndIsDeletedFalse(idem).isPresent()) continue;

            String severity = daysLeft <= 0 ? "CRITICAL" : (daysLeft <= 1 ? "CRITICAL" : "WARNING");
            StockAlert alert = StockAlert.builder()
                    .warehouseId(batch.getWarehouseId())
                    .productId(batch.getProductId())
                    .currentQty(batch.getQtyOnHand())
                    .minQty(0.0)
                    .severity(severity)
                    .status("OPEN")
                    .triggeredAt(LocalDateTime.now())
                    .idempotencyKey(idem)
                    .alertType("EXPIRY_SOON")
                    .batchId(batch.getId())
                    .expiryDate(batch.getExpiryDate())
                    .daysToExpiry(daysLeft)
                    .build();
            alert.setId(UUID.randomUUID().toString());
            alertRepository.save(alert);
            raised++;
        }
        log.info("[ExpiryAlert] scan done — raised {} expiry alerts from {} active batches", raised, batches.size());
        return raised;
    }

    private int resolveExpiryAlertDays(String productId) {
        Product p = productRepository.findById(productId).orElse(null);
        if (p != null && p.getExpiryAlertDays() != null && p.getExpiryAlertDays() > 0) {
            return p.getExpiryAlertDays();
        }
        String catCode = p != null ? resolveCategoryCode(p.getCategoryId()) : null;
        if ("LSP_VEG".equals(catCode)) {
            return DEFAULT_ALERT_LEAFY;
        }
        return DEFAULT_ALERT_ROOT;
    }

    private String resolveCategoryCode(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT code FROM categories WHERE id = ? AND COALESCE(is_deleted, false) = false LIMIT 1",
                    String.class, categoryId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }
}
