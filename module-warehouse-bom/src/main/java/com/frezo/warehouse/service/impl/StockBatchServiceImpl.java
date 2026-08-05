package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.response.FePage;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.common.WarehouseErrorCode;
import com.frezo.warehouse.dto.response.FefoBatchSuggestion;
import com.frezo.warehouse.dto.response.FefoSuggestResponse;
import com.frezo.warehouse.dto.response.StockBatchResponse;
import com.frezo.warehouse.entity.StockBatch;
import com.frezo.warehouse.entity.Warehouse;
import com.frezo.warehouse.entity.WarehouseLocation;
import com.frezo.warehouse.repository.StockBatchRepository;
import com.frezo.warehouse.repository.WarehouseLocationRepository;
import com.frezo.warehouse.repository.WarehouseRepository;
import com.frezo.warehouse.service.StockBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockBatchServiceImpl implements StockBatchService {

    private static final int SHELF_LIFE_LEAFY_DAYS = 3;
    private static final int SHELF_LIFE_ROOT_DAYS = 7;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final StockBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public StockBatchResponse getById(String id) {
        StockBatch batch = batchRepository.findById(id)
                .filter(b -> Boolean.FALSE.equals(b.getIsDeleted()))
                .orElseThrow(() -> new AppException(WarehouseErrorCode.BATCH_NOT_FOUND));
        return toResponse(batch);
    }

    @Override
    public FePage<StockBatchResponse> list(
            String warehouseId,
            String productId,
            String status,
            String keyword,
            LocalDate expiryFrom,
            LocalDate expiryTo) {
        List<StockBatch> batches;
        boolean hasWh = warehouseId != null && !warehouseId.isBlank();
        boolean hasProduct = productId != null && !productId.isBlank();
        if (hasWh && hasProduct) {
            batches = batchRepository.findByWarehouseIdAndProductIdAndIsDeletedFalseOrderByExpiryDateAsc(
                    warehouseId, productId);
        } else if (hasWh) {
            batches = batchRepository.findByWarehouseIdAndIsDeletedFalseOrderByExpiryDateAsc(warehouseId);
        } else if (hasProduct) {
            batches = batchRepository.findByProductIdAndIsDeletedFalseOrderByExpiryDateAsc(productId);
        } else {
            batches = batchRepository.findAll().stream()
                    .filter(b -> Boolean.FALSE.equals(b.getIsDeleted()))
                    .toList();
        }

        String statusKey = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
        String kw = keyword != null && !keyword.isBlank() ? keyword.trim().toLowerCase() : null;

        List<StockBatchResponse> rows = batches.stream()
                .map(this::toResponse)
                .filter(r -> statusKey == null
                        || (r.getStatus() != null && statusKey.equals(r.getStatus().toUpperCase())))
                .filter(r -> matchesKeyword(r, kw))
                .filter(r -> matchesExpiryRange(r.getExpiryDate(), expiryFrom, expiryTo))
                .toList();
        return FePage.all(rows);
    }

    private static boolean matchesKeyword(StockBatchResponse r, String kw) {
        if (kw == null) return true;
        return containsIgnoreCase(r.getBatchCode(), kw)
                || containsIgnoreCase(r.getProductCode(), kw)
                || containsIgnoreCase(r.getProductName(), kw)
                || containsIgnoreCase(r.getSupplierName(), kw)
                || containsIgnoreCase(r.getLocationLabel(), kw);
    }

    private static boolean containsIgnoreCase(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }

    private static boolean matchesExpiryRange(String expiryIso, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (expiryIso == null || expiryIso.isBlank()) return false;
        LocalDate expiry;
        try {
            expiry = LocalDate.parse(expiryIso, DATE_FMT);
        } catch (Exception ex) {
            return false;
        }
        if (from != null && expiry.isBefore(from)) return false;
        if (to != null && expiry.isAfter(to)) return false;
        return true;
    }

    @Override
    public FefoSuggestResponse suggestFefo(String warehouseId, String productId, double qty) {
        List<StockBatch> available = batchRepository.findAvailableForFefo(warehouseId, productId);
        List<FefoBatchSuggestion> suggestions = new ArrayList<>();
        double remaining = qty;
        double allocated = 0;

        for (StockBatch batch : available) {
            if (remaining <= 0) break;
            double onHand = batch.getQtyOnHand() != null ? batch.getQtyOnHand() : 0;
            if (onHand <= 0) continue;

            double take = Math.min(remaining, onHand);
            Integer days = daysToExpiry(batch.getExpiryDate());
            suggestions.add(FefoBatchSuggestion.builder()
                    .batchId(batch.getId())
                    .batchCode(batch.getBatchCode())
                    .expiryDate(batch.getExpiryDate() != null ? batch.getExpiryDate().format(DATE_FMT) : null)
                    .qtyAvailable(onHand)
                    .suggestedQty(take)
                    .daysToExpiry(days)
                    .expiryWarning(expiryWarning(days))
                    .supplierName(resolveSupplierName(batch.getSupplierId()))
                    .build());
            remaining -= take;
            allocated += take;
        }

        return FefoSuggestResponse.builder()
                .productId(productId)
                .warehouseId(warehouseId)
                .requestedQty(qty)
                .allocatedQty(allocated)
                .suggestions(suggestions)
                .build();
    }

    @Override
    public LocalDate computeExpiryDate(String productId, LocalDate receivedDate) {
        LocalDate base = receivedDate != null ? receivedDate : LocalDate.now();
        int shelfDays = resolveShelfLifeDays(productId);
        return base.plusDays(shelfDays);
    }

    @Override
    public String generateBatchCode(String productId, String supplierId, LocalDate receivedDate) {
        String productCode = productRepository.findById(productId)
                .map(Product::getCode)
                .orElse("SP");
        String supplierCode = resolveSupplierCode(supplierId);
        String datePart = (receivedDate != null ? receivedDate : LocalDate.now())
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "LOT-" + productCode + "-" + supplierCode + "-" + datePart;
        long sameDay = batchRepository.countByBatchCodePrefix(prefix);
        return prefix + "-" + String.format("%03d", sameDay + 1);
    }

    int resolveShelfLifeDays(String productId) {
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null) return SHELF_LIFE_ROOT_DAYS;
        String catCode = resolveCategoryCode(p.getCategoryId());
        if ("LSP_VEG".equals(catCode)) {
            return SHELF_LIFE_LEAFY_DAYS;
        }
        return SHELF_LIFE_ROOT_DAYS;
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

    private String resolveSupplierCode(String supplierId) {
        if (supplierId == null || supplierId.isBlank()) return "NCC";
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT code FROM nccs WHERE id = ? AND COALESCE(is_deleted, false) = false LIMIT 1",
                    String.class, supplierId);
        } catch (EmptyResultDataAccessException ex) {
            return "NCC";
        }
    }

    private String resolveSupplierName(String supplierId) {
        if (supplierId == null || supplierId.isBlank()) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT name FROM nccs WHERE id = ? AND COALESCE(is_deleted, false) = false LIMIT 1",
                    String.class, supplierId);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private StockBatchResponse toResponse(StockBatch b) {
        Product p = productRepository.findById(b.getProductId()).orElse(null);
        Warehouse wh = warehouseRepository.findById(b.getWarehouseId()).orElse(null);
        Integer days = daysToExpiry(b.getExpiryDate());
        String locationLabel = null;
        if (b.getWarehouseLocationId() != null) {
            locationLabel = locationRepository.findById(b.getWarehouseLocationId())
                    .map(this::formatLocation)
                    .orElse(null);
        }
        return StockBatchResponse.builder()
                .id(b.getId())
                .batchCode(b.getBatchCode())
                .productId(b.getProductId())
                .productCode(p != null ? p.getCode() : null)
                .productName(p != null ? p.getName() : null)
                .warehouseId(b.getWarehouseId())
                .warehouseName(wh != null ? wh.getName() : null)
                .supplierId(b.getSupplierId())
                .supplierName(resolveSupplierName(b.getSupplierId()))
                .grnId(b.getGrnId())
                .grnItemId(b.getGrnItemId())
                .warehouseLocationId(b.getWarehouseLocationId())
                .locationLabel(locationLabel)
                .receivedDate(b.getReceivedDate() != null ? b.getReceivedDate().format(DATE_FMT) : null)
                .expiryDate(b.getExpiryDate() != null ? b.getExpiryDate().format(DATE_FMT) : null)
                .qtyOnHand(b.getQtyOnHand())
                .status(b.getStatus())
                .daysToExpiry(days)
                .expiryWarning(expiryWarning(days))
                .build();
    }

    private String formatLocation(WarehouseLocation loc) {
        return String.format("%s-%s-L%s-B%s",
                loc.getAisle() != null ? loc.getAisle() : "",
                loc.getRack() != null ? loc.getRack() : "",
                loc.getLevel() != null ? loc.getLevel() : "",
                loc.getBin() != null ? loc.getBin() : "");
    }

    static Integer daysToExpiry(LocalDate expiryDate) {
        if (expiryDate == null) return null;
        return (int) ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    static String expiryWarning(Integer days) {
        if (days == null) return null;
        if (days <= 0) return "Đã hết hạn";
        if (days == 1) return "Cận hạn 1 ngày";
        if (days <= 3) return "Cận hạn " + days + " ngày";
        return null;
    }
}
