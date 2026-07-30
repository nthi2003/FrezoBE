package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.FePage;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.dto.request.ReorderRuleRequest;
import com.frezo.warehouse.dto.response.ReorderRuleDto;
import com.frezo.warehouse.dto.response.StockAlertDto;
import com.frezo.warehouse.dto.response.WarehouseOptionDto;
import com.frezo.warehouse.entity.ReorderRule;
import com.frezo.warehouse.entity.StockAlert;
import com.frezo.warehouse.entity.Warehouse;
import com.frezo.warehouse.repository.ReorderRuleRepository;
import com.frezo.warehouse.repository.StockAlertRepository;
import com.frezo.warehouse.repository.StockBalanceRepository;
import com.frezo.warehouse.repository.StockBatchRepository;
import com.frezo.warehouse.repository.WarehouseRepository;
import com.frezo.warehouse.service.ExpiryAlertService;
import com.frezo.warehouse.service.ReorderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reorder rules + stock alerts. Enrich product/warehouse inline (≤5 deps).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReorderServiceImpl implements ReorderService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ReorderRuleRepository ruleRepository;
    private final StockAlertRepository alertRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockBatchRepository stockBatchRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ExpiryAlertService expiryAlertService;

    @Override
    public List<WarehouseOptionDto> listWarehouses() {
        return warehouseRepository.findAll().stream()
                .filter(w -> Boolean.FALSE.equals(w.getIsDeleted()) || w.getIsDeleted() == null)
                .map(w -> WarehouseOptionDto.builder()
                        .id(w.getId()).name(w.getName()).code(w.getCode()).build())
                .toList();
    }

    @Override
    public FePage<ReorderRuleDto> listRules(String warehouseId, String productId) {
        List<ReorderRuleDto> list = ruleRepository.findByIsDeletedFalseOrderByUpdatedDateDesc().stream()
                .filter(r -> warehouseId == null || warehouseId.equals(r.getWarehouseId()))
                .filter(r -> productId == null || productId.equals(r.getProductId()))
                .map(this::toRuleDto)
                .toList();
        return FePage.all(list);
    }

    @Override
    @Transactional
    public ReorderRuleDto create(ReorderRuleRequest req) {
        ruleRepository.findByProductIdAndWarehouseIdAndIsDeletedFalse(req.getProductId(), req.getWarehouseId())
                .ifPresent(x -> {
                    throw new AppException(CommonErrorCode.CONFLICT, "Rule đã tồn tại cho product+warehouse");
                });
        ReorderRule r = ReorderRule.builder()
                .warehouseId(req.getWarehouseId())
                .productId(req.getProductId())
                .minQty(req.getMinQty())
                .maxQty(req.getMaxQty())
                .reorderQty(req.getReorderQty())
                .active(req.getActive() == null || req.getActive())
                .build();
        r.setId(UUID.randomUUID().toString());
        return toRuleDto(ruleRepository.save(r));
    }

    @Override
    @Transactional
    public ReorderRuleDto update(String id, ReorderRuleRequest req) {
        ReorderRule r = ruleRepository.findById(id)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Reorder rule không tồn tại"));
        if (req.getMinQty() != null) r.setMinQty(req.getMinQty());
        if (req.getMaxQty() != null) r.setMaxQty(req.getMaxQty());
        if (req.getReorderQty() != null) r.setReorderQty(req.getReorderQty());
        if (req.getActive() != null) r.setActive(req.getActive());
        if (req.getWarehouseId() != null) r.setWarehouseId(req.getWarehouseId());
        if (req.getProductId() != null) r.setProductId(req.getProductId());
        return toRuleDto(ruleRepository.save(r));
    }

    @Override
    @Transactional
    public void delete(String id) {
        ReorderRule r = ruleRepository.findById(id)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Reorder rule không tồn tại"));
        r.softDelete(SystemUtils.getCurrentUsername());
        ruleRepository.save(r);
    }

    @Override
    public Map<String, Integer> importExcel(MultipartFile file) {
        // MVP stub — FE wizard có thể parse client-side; BE trả 0 để không 501.
        log.info("[Reorder] import-excel stub, file={}", file != null ? file.getOriginalFilename() : null);
        Map<String, Integer> result = new HashMap<>();
        result.put("imported", 0);
        return result;
    }

    @Override
    public FePage<StockAlertDto> listAlerts(String status, String alertType) {
        expiryAlertService.scanAndRaiseExpiryAlerts();

        List<StockAlert> alerts;
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) {
            alerts = alertRepository.findByIsDeletedFalseOrderByTriggeredAtDesc();
        } else {
            String st = status.toUpperCase();
            if ("OPEN".equals(st)) {
                alerts = alertRepository.findByStatusAndIsDeletedFalseOrderByTriggeredAtDesc("OPEN");
            } else {
                alerts = alertRepository.findByIsDeletedFalseOrderByTriggeredAtDesc().stream()
                        .filter(a -> !"OPEN".equals(a.getStatus()))
                        .toList();
            }
        }
        if (alertType != null && !alertType.isBlank()) {
            String type = alertType.toUpperCase();
            alerts = alerts.stream()
                    .filter(a -> type.equals(a.getAlertType() != null ? a.getAlertType() : "LOW_STOCK"))
                    .toList();
        }
        return FePage.all(alerts.stream().map(this::toAlertDto).toList());
    }

    @Override
    @Transactional
    public StockAlertDto dismiss(String id) {
        StockAlert a = alertRepository.findById(id)
                .filter(x -> Boolean.FALSE.equals(x.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Alert không tồn tại"));
        a.setStatus("DISMISSED");
        a.setDismissedAt(LocalDateTime.now());
        return toAlertDto(alertRepository.save(a));
    }

    @Override
    @Transactional
    public void scanAndRaiseAlerts() {
        List<ReorderRule> rules = ruleRepository.findByActiveTrueAndIsDeletedFalse();
        String dayKey = LocalDate.now().toString().replace("-", "");
        int raised = 0;
        for (ReorderRule rule : rules) {
            Double qty = stockBalanceRepository
                    .sumAvailableByProductAndWarehouse(rule.getProductId(), rule.getWarehouseId());
            double current = qty != null ? qty : 0.0;
            double min = rule.getMinQty() != null ? rule.getMinQty() : 0.0;
            if (current >= min) continue;

            String idem = rule.getProductId() + "|" + rule.getWarehouseId() + "|" + dayKey;
            if (alertRepository.findByIdempotencyKeyAndIsDeletedFalse(idem).isPresent()) continue;
            if (alertRepository.findByProductIdAndWarehouseIdAndStatusAndIsDeletedFalse(
                    rule.getProductId(), rule.getWarehouseId(), "OPEN").isPresent()) continue;

            String severity = current <= 0 ? "CRITICAL" : "WARNING";
            StockAlert alert = StockAlert.builder()
                    .warehouseId(rule.getWarehouseId())
                    .productId(rule.getProductId())
                    .currentQty(current)
                    .minQty(min)
                    .severity(severity)
                    .status("OPEN")
                    .triggeredAt(LocalDateTime.now())
                    .idempotencyKey(idem)
                    .alertType("LOW_STOCK")
                    .build();
            alert.setId(UUID.randomUUID().toString());
            alertRepository.save(alert);
            raised++;
        }
        log.info("[Reorder] scan done — raised {} alerts from {} rules", raised, rules.size());
    }

    private ReorderRuleDto toRuleDto(ReorderRule r) {
        Warehouse wh = warehouseRepository.findById(r.getWarehouseId()).orElse(null);
        Product p = productRepository.findById(r.getProductId()).orElse(null);
        return ReorderRuleDto.builder()
                .id(r.getId())
                .warehouseId(r.getWarehouseId())
                .warehouseName(wh != null ? wh.getName() : null)
                .productId(r.getProductId())
                .productCode(p != null ? p.getCode() : null)
                .productName(p != null ? p.getName() : null)
                .categoryName(p != null ? resolveCategoryName(p.getCategoryId()) : null)
                .minQty(r.getMinQty())
                .maxQty(r.getMaxQty())
                .reorderQty(r.getReorderQty())
                .active(Boolean.TRUE.equals(r.getActive()))
                .updatedAt(r.getUpdatedDate() != null ? r.getUpdatedDate().format(ISO) : null)
                .build();
    }

    private String resolveCategoryName(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        try {
            return jdbcTemplate.queryForObject(
                    """
                    SELECT name FROM categories
                    WHERE id = ? AND COALESCE(is_deleted, false) = false
                    LIMIT 1
                    """,
                    String.class,
                    categoryId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private StockAlertDto toAlertDto(StockAlert a) {
        Warehouse wh = warehouseRepository.findById(a.getWarehouseId()).orElse(null);
        Product p = productRepository.findById(a.getProductId()).orElse(null);
        String batchCode = null;
        if (a.getBatchId() != null) {
            batchCode = stockBatchRepository.findById(a.getBatchId())
                    .map(b -> b.getBatchCode())
                    .orElse(null);
        }
        return StockAlertDto.builder()
                .id(a.getId())
                .warehouseId(a.getWarehouseId())
                .warehouseName(wh != null ? wh.getName() : null)
                .productId(a.getProductId())
                .productCode(p != null ? p.getCode() : null)
                .productName(p != null ? p.getName() : null)
                .categoryName(p != null ? resolveCategoryName(p.getCategoryId()) : null)
                .currentQty(a.getCurrentQty())
                .minQty(a.getMinQty())
                .severity(a.getSeverity())
                .status(a.getStatus())
                .triggeredAt(a.getTriggeredAt() != null ? a.getTriggeredAt().format(ISO) : null)
                .dismissedAt(a.getDismissedAt() != null ? a.getDismissedAt().format(ISO) : null)
                .alertType(a.getAlertType() != null ? a.getAlertType() : "LOW_STOCK")
                .batchId(a.getBatchId())
                .batchCode(batchCode)
                .expiryDate(a.getExpiryDate() != null ? a.getExpiryDate().toString() : null)
                .daysToExpiry(a.getDaysToExpiry())
                .build();
    }
}
