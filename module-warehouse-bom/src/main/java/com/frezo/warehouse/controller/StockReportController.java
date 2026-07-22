package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.service.StockReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RestController
@RequestMapping("/warehouse/report")
@RequiredArgsConstructor
@Tag(name = "35. Báo cáo kho", description = "API báo cáo nhập-xuất-tồn, xuất Excel")
public class StockReportController {

    private final StockReportService stockReportService;

    @Operation(summary = "Báo cáo nhập-xuất-tồn", description = "Lọc theo ngày, sản phẩm, kho")
    @GetMapping("/stock-movement")
    public ApiResponse<?> getStockMovementReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String warehouseId) {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        return ApiResponse.success(stockReportService.getStockMovementReport(from, to, productId, warehouseId));
    }

    @Operation(summary = "Cảnh báo tồn kho thấp", description = "Danh sách sản phẩm có số lượng tồn dưới ngưỡng cảnh báo")
    @GetMapping("/low-stock")
    public ApiResponse<?> getLowStockAlerts() {
        return ApiResponse.success(stockReportService.getLowStockAlerts());
    }

    @Operation(summary = "Xuất Excel nhập-xuất-tồn")
    @GetMapping("/stock-movement/export")
    public void exportStockMovement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String warehouseId,
            HttpServletResponse response) throws IOException {
        LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;
        byte[] data = stockReportService.exportStockMovementExcel(from, to, productId, warehouseId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=nhap_xuat_ton.xlsx");
        response.getOutputStream().write(data);
    }
}
