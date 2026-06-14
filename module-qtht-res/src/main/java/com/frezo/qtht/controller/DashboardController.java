package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.dto.response.DashboardSummaryResponse;
import com.frezo.qtht.service.DashboardService;
import com.frezo.qtht.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/qtht/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard API", description = "Aggregated dashboard data")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReportService reportService;

    @GetMapping("/summary")
    @Operation(summary = "Get summary data for the main dashboard")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }

    @GetMapping("/export/attendance")
    @Operation(summary = "Export monthly attendance report to Excel")
    public void exportAttendance(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        byte[] excelData = reportService.exportMonthlyAttendance(LocalDate.now().getMonthValue(), LocalDate.now().getYear());
        
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=attendance_report.xlsx");
        response.getOutputStream().write(excelData);
    }
}
