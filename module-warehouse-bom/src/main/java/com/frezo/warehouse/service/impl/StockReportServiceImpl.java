package com.frezo.warehouse.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.product.entity.Product;
import com.frezo.product.repository.ProductRepository;
import com.frezo.warehouse.dto.response.LowStockAlertResponse;
import com.frezo.warehouse.dto.response.StockMovementReportResponse;
import com.frezo.warehouse.dto.response.StockMovementResponse;
import com.frezo.warehouse.entity.StockBalance;
import com.frezo.warehouse.entity.StockLedger;
import com.frezo.warehouse.repository.StockBalanceRepository;
import com.frezo.warehouse.repository.StockLedgerRepository;
import com.frezo.warehouse.service.StockReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockReportServiceImpl implements StockReportService {

    private final StockLedgerRepository stockLedgerRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final ProductRepository productRepository;

    @Override
    public List<StockMovementReportResponse> getStockMovementReport(
            LocalDateTime from, LocalDateTime to, String productId, String warehouseId) {
        if (from == null) from = LocalDate.now().minusMonths(1).atStartOfDay();
        if (to == null) to = LocalDate.now().atTime(LocalTime.MAX);

        List<StockLedger> ledgers = stockLedgerRepository.searchMovements(
                from, to, productId, warehouseId, null);

        Map<String, List<StockLedger>> grouped = ledgers.stream()
                .collect(Collectors.groupingBy(StockLedger::getProductId, LinkedHashMap::new, Collectors.toList()));

        List<StockMovementReportResponse> reports = new ArrayList<>();
        for (Map.Entry<String, List<StockLedger>> entry : grouped.entrySet()) {
            StockMovementReportResponse report = new StockMovementReportResponse();
            report.setProductId(entry.getKey());

            List<StockLedger> productLedgers = entry.getValue();
            double runningBalance = 0;
            List<StockMovementResponse> movements = new ArrayList<>();
            double totalIn = 0, totalOut = 0;

            for (StockLedger ledger : productLedgers) {
                double qty = ledger.getQuantity() != null ? ledger.getQuantity() : 0;
                if ("IN".equals(ledger.getTransactionType())) {
                    runningBalance += qty;
                    totalIn += qty;
                } else if ("OUT".equals(ledger.getTransactionType())) {
                    runningBalance -= qty;
                    totalOut += qty;
                }

                StockMovementResponse mv = new StockMovementResponse();
                mv.setId(ledger.getId());
                mv.setProductId(ledger.getProductId());
                mv.setBatchId(ledger.getBatchId());
                mv.setWarehouseId(ledger.getWarehouseId());
                mv.setLocationId(ledger.getLocationId());
                mv.setTransactionType(ledger.getTransactionType());
                mv.setQuantity(qty);
                mv.setUnitCost(ledger.getUnitCost());
                mv.setTotalValue(ledger.getTotalValue());
                mv.setReferenceType(ledger.getReferenceType());
                mv.setReferenceId(ledger.getReferenceId());
                mv.setNote(ledger.getNote());
                mv.setCreatedDate(ledger.getCreatedDate());
                mv.setCreatedBy(ledger.getCreatedBy());
                mv.setRunningBalance(runningBalance);
                movements.add(mv);
            }

            report.setMovements(movements);
            report.setTotalIn(totalIn);
            report.setTotalOut(totalOut);
            report.setClosingBalance(runningBalance);
            report.setOpeningBalance(runningBalance + totalOut - totalIn);

            reports.add(report);
        }
        return reports;
    }

    @Override
    public byte[] exportStockMovementExcel(
            LocalDateTime from, LocalDateTime to, String productId, String warehouseId) {
        if (from == null) from = LocalDate.now().minusMonths(1).atStartOfDay();
        if (to == null) to = LocalDate.now().atTime(LocalTime.MAX);

        List<StockLedger> ledgers = stockLedgerRepository.searchMovements(
                from, to, productId, warehouseId, null);

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Nhap xuat ton");
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

            Row header = sheet.createRow(0);
            String[] cols = {"STT", "Ngày", "Sản phẩm", "Kho", "Loại GD", "SL Nhập", "SL Xuất", "Tồn sau GD",
                    "Đơn giá", "Thành tiền", "CT Tham chiếu", "Mã TK", "Người tạo", "Ghi chú"};
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            double runningBalance = 0;
            for (StockLedger l : ledgers) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(l.getCreatedDate() != null ? l.getCreatedDate().format(dtf) : "");
                row.createCell(2).setCellValue(l.getProductId());
                row.createCell(3).setCellValue(l.getWarehouseId());
                row.createCell(4).setCellValue(l.getTransactionType());
                double qty = l.getQuantity() != null ? l.getQuantity() : 0;
                if ("IN".equals(l.getTransactionType())) {
                    row.createCell(5).setCellValue(qty);
                    runningBalance += qty;
                } else if ("OUT".equals(l.getTransactionType())) {
                    row.createCell(6).setCellValue(qty);
                    runningBalance -= qty;
                }
                row.createCell(7).setCellValue(runningBalance);
                row.createCell(8).setCellValue(l.getUnitCost() != null ? l.getUnitCost() : 0);
                row.createCell(9).setCellValue(l.getTotalValue() != null ? l.getTotalValue() : 0);
                row.createCell(10).setCellValue(l.getReferenceType() + "/" + (l.getReferenceId() != null ? l.getReferenceId().substring(0, Math.min(8, l.getReferenceId().length())) : ""));
                row.createCell(11).setCellValue(l.getReferenceId() != null ? l.getReferenceId() : "");
                row.createCell(12).setCellValue(l.getCreatedBy() != null ? l.getCreatedBy() : "");
                row.createCell(13).setCellValue(l.getNote() != null ? l.getNote() : "");
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
            LowStockAlertResponse r = new LowStockAlertResponse();
            r.setProductId(sb.getProductId());
            r.setWarehouseId(sb.getWarehouseId());
            r.setLocationId(sb.getLocationId());
            r.setBatchId(sb.getBatchId());
            r.setQuantityAvailable(sb.getQuantityAvailable());
            r.setWarningThreshold(thresholds.get(sb.getProductId()));
            return r;
        }).toList();
    }
}
