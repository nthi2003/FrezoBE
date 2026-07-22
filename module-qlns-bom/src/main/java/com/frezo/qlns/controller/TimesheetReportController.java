package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.service.TimesheetReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/qlns/report")
@RequiredArgsConstructor
@Tag(name = "27. Báo cáo chấm công", description = "API bảng chấm công tháng + xuất Excel")
public class TimesheetReportController {

    private final TimesheetReportService timesheetReportService;

    @Operation(summary = "Bảng chấm công tháng", description = "Lọc theo tháng, năm, nhân viên")
    @GetMapping("/timesheet")
    public ApiResponse<?> getTimesheet(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) String personId) {
        return ApiResponse.success(timesheetReportService.getTimesheet(month, year, personId));
    }

    @Operation(summary = "Xuất Excel bảng chấm công")
    @GetMapping("/timesheet/export")
    public void exportTimesheet(
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam(required = false) String personId,
            HttpServletResponse response) throws IOException {
        byte[] data = timesheetReportService.exportTimesheetExcel(month, year, personId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=bang_cham_cong_T" + month + "_" + year + ".xlsx");
        response.getOutputStream().write(data);
    }
}
