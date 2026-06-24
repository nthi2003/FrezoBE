package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.response.PageResponse;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.dto.response.LowStockAlertResponse;
import com.frezo.warehouse.dto.response.StockBalanceResponse;
import com.frezo.warehouse.dto.response.WarehouseStatsResponse;
import com.frezo.warehouse.entity.StockBalance;
import com.frezo.warehouse.repository.*;
import com.frezo.warehouse.service.StockBalanceService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockBalanceServiceImpl implements StockBalanceService {

    private final StockBalanceRepository stockBalanceRepository;
    private final GoodsReceiptNoteRepository grnRepository;
    private final GoodsIssueNoteRepository ginRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockLedgerRepository stockLedgerRepository;
    private final ProductRepository productRepository;

    @Override
    public PageResponse<StockBalanceResponse> filter(String warehouseId, String productId, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<StockBalance> balancePage = stockBalanceRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (warehouseId != null && !warehouseId.isBlank()) {
                predicates.add(cb.equal(root.get("warehouseId"), warehouseId));
            }
            if (productId != null && !productId.isBlank()) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        List<StockBalanceResponse> responses = balancePage.getContent().stream().map(this::toResponse).toList();
        return PageResponse.of(page, size, balancePage, responses);
    }

    @Override
    public StockBalanceResponse getById(String id) {
        StockBalance balance = stockBalanceRepository.findById(id)
                .orElseThrow(() -> new QTHTException("stock.balance.not.found", HttpStatus.NOT_FOUND));
        return toResponse(balance);
    }

    @Override
    public List<LowStockAlertResponse> getLowStockAlerts() {
        List<String> ids = stockBalanceRepository.findLowStockBalanceIds();
        if (ids.isEmpty()) return List.of();

        List<StockBalance> items = stockBalanceRepository.findAllById(ids);

        Map<String, Double> thresholds = productRepository.findAllById(
                items.stream().map(StockBalance::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p ->
                p.getWarningThreshold() != null ? p.getWarningThreshold() : 0.0, (a, b) -> a));

        return items.stream().map(sb -> {
            LowStockAlertResponse alert = new LowStockAlertResponse();
            alert.setProductId(sb.getProductId());
            alert.setWarehouseId(sb.getWarehouseId());
            alert.setLocationId(sb.getLocationId());
            alert.setBatchId(sb.getBatchId());
            alert.setQuantityAvailable(sb.getQuantityAvailable());
            alert.setWarningThreshold(thresholds.get(sb.getProductId()));
            return alert;
        }).toList();
    }

    @Override
    public WarehouseStatsResponse getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime endToday = today.atTime(LocalTime.MAX);

        long totalWarehouses = warehouseRepository.count();
        long totalProductsInStock = stockBalanceRepository.countDistinctProductId();
        long lowStockAlerts = stockBalanceRepository.findLowStockBalanceIds().size();
        long grnToday = grnRepository.countByCreatedDateBetween(startToday, endToday);
        long ginToday = ginRepository.countByCreatedDateBetween(startToday, endToday);

        return WarehouseStatsResponse.builder()
                .totalWarehouses(totalWarehouses)
                .totalProductsInStock(totalProductsInStock)
                .lowStockAlerts(lowStockAlerts)
                .grnToday(grnToday)
                .ginToday(ginToday)
                .build();
    }

    @Override
    public byte[] exportStock(String warehouseId, String productId) {
        List<StockBalance> balances;
        if (warehouseId != null && !warehouseId.isBlank()) {
            balances = stockBalanceRepository.findAll((root, query, cb) ->
                    cb.equal(root.get("warehouseId"), warehouseId));
        } else {
            balances = stockBalanceRepository.findAll();
        }

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Ton kho");
            Row header = sheet.createRow(0);
            String[] cols = {"STT", "Product ID", "Kho", "Vị trí", "Lô", "Tồn", "Đã giữ", "Khả dụng"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                CellStyle style = wb.createCellStyle();
                Font font = wb.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            int rowNum = 1;
            for (StockBalance sb : balances) {
                if (productId != null && !productId.isBlank() && !productId.equals(sb.getProductId())) {
                    continue;
                }
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(sb.getProductId());
                row.createCell(2).setCellValue(sb.getWarehouseId());
                row.createCell(3).setCellValue(sb.getLocationId());
                row.createCell(4).setCellValue(sb.getBatchId());
                row.createCell(5).setCellValue(sb.getQuantityOnHand());
                row.createCell(6).setCellValue(sb.getQuantityReserved());
                row.createCell(7).setCellValue(sb.getQuantityAvailable());
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new QTHTException("stock.export.failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private StockBalanceResponse toResponse(StockBalance balance) {
        StockBalanceResponse r = new StockBalanceResponse();
        r.setId(balance.getId());
        r.setProductId(balance.getProductId());
        r.setWarehouseId(balance.getWarehouseId());
        r.setLocationId(balance.getLocationId());
        r.setBatchId(balance.getBatchId());
        r.setQuantityOnHand(balance.getQuantityOnHand());
        r.setQuantityReserved(balance.getQuantityReserved());
        r.setQuantityAvailable(balance.getQuantityAvailable());
        return r;
    }
}
