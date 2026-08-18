package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.dto.response.ProductStockChartPoint;
import com.frezo.warehouse.dto.response.ProductStockLevelDto;
import com.frezo.warehouse.dto.response.ProductWarehouseContextDto;
import com.frezo.warehouse.dto.response.ReorderRuleDto;
import com.frezo.warehouse.entity.StockBalance;
import com.frezo.warehouse.entity.StockLedger;
import com.frezo.warehouse.entity.Warehouse;
import com.frezo.warehouse.repository.StockBalanceRepository;
import com.frezo.warehouse.repository.StockLedgerRepository;
import com.frezo.warehouse.repository.WarehouseRepository;
import com.frezo.warehouse.service.ProductWarehouseService;
import com.frezo.warehouse.service.ReorderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductWarehouseServiceImpl implements ProductWarehouseService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int DEFAULT_DAYS = 90;

    private final ProductRepository productRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final ReorderService reorderService;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductWarehouseContextDto getContext(String productId, Integer days, String warehouseId) {
        ensureProductExists(productId);
        List<ProductStockLevelDto> levels = getStockLevels(productId);
        double totalOnHand = levels.stream().mapToDouble(l -> safe(l.getQuantityOnHand())).sum();
        double totalAvailable = levels.stream().mapToDouble(l -> safe(l.getQuantityAvailable())).sum();
        double totalReserved = levels.stream().mapToDouble(l -> safe(l.getQuantityReserved())).sum();

        return ProductWarehouseContextDto.builder()
                .productId(productId)
                .totalOnHand(totalOnHand)
                .totalAvailable(totalAvailable)
                .totalReserved(totalReserved)
                .stockChart(getStockChart(productId, days, warehouseId))
                .stockLevels(levels)
                .reorderRules(getReorderRules(productId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductStockChartPoint> getStockChart(String productId, Integer days, String warehouseId) {
        ensureProductExists(productId);
        int windowDays = days != null && days > 0 ? days : DEFAULT_DAYS;
        LocalDateTime from = LocalDateTime.now().minusDays(windowDays);

        double opening = sumQuantity(
                stockLedgerRepository.searchMovements(null, from.minusNanos(1), productId, warehouseId, null));

        List<StockLedger> movements = stockLedgerRepository.searchMovements(
                from, null, productId, warehouseId, null);

        List<ProductStockChartPoint> points = new ArrayList<>();
        points.add(ProductStockChartPoint.builder()
                .date(from.format(ISO))
                .quantityOnHand(opening)
                .transactionType("OPENING")
                .deltaQty(0d)
                .note("Tồn đầu kỳ")
                .build());

        double running = opening;
        for (StockLedger m : movements) {
            running += safe(m.getQuantity());
            points.add(ProductStockChartPoint.builder()
                    .date(m.getCreatedDate() != null ? m.getCreatedDate().format(ISO) : null)
                    .quantityOnHand(running)
                    .transactionType(m.getTransactionType())
                    .deltaQty(m.getQuantity())
                    .referenceType(m.getReferenceType())
                    .note(m.getNote())
                    .build());
        }
        return points;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductStockLevelDto> getStockLevels(String productId) {
        ensureProductExists(productId);
        Map<String, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .filter(w -> w.getIsDeleted() == null || !w.getIsDeleted())
                .collect(Collectors.toMap(Warehouse::getId, w -> w, (a, b) -> a));

        Map<String, List<StockBalance>> byWarehouse = stockBalanceRepository
                .findByProductIdAndIsDeletedFalse(productId).stream()
                .collect(Collectors.groupingBy(StockBalance::getWarehouseId));

        return byWarehouse.entrySet().stream()
                .map(e -> {
                    Warehouse wh = warehouseMap.get(e.getKey());
                    double onHand = e.getValue().stream().mapToDouble(b -> safe(b.getQuantityOnHand())).sum();
                    double reserved = e.getValue().stream().mapToDouble(b -> safe(b.getQuantityReserved())).sum();
                    double available = e.getValue().stream().mapToDouble(b -> safe(b.getQuantityAvailable())).sum();
                    return ProductStockLevelDto.builder()
                            .warehouseId(e.getKey())
                            .warehouseCode(wh != null ? wh.getCode() : null)
                            .warehouseName(wh != null ? wh.getName() : e.getKey())
                            .quantityOnHand(onHand)
                            .quantityReserved(reserved)
                            .quantityAvailable(available)
                            .build();
                })
                .sorted(Comparator.comparing(ProductStockLevelDto::getWarehouseName,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReorderRuleDto> getReorderRules(String productId) {
        ensureProductExists(productId);
        return reorderService.listRules(null, productId).getContent();
    }

    private void ensureProductExists(String productId) {
        productRepository.findById(productId)
                .filter(p -> p.getIsDeleted() == null || !p.getIsDeleted())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy sản phẩm"));
    }

    private static double sumQuantity(List<StockLedger> entries) {
        return entries.stream().mapToDouble(e -> safe(e.getQuantity())).sum();
    }

    private static double safe(Double v) {
        return v != null ? v : 0d;
    }
}
