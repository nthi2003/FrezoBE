package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.AttendanceCheckInRequest;
import com.frezo.qlns.dto.request.AttendanceCheckOutRequest;
import com.frezo.qlns.dto.request.AttendanceFilter;
import com.frezo.qlns.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qlns/attendance")
@RequiredArgsConstructor
@Tag(name = "Quản lý chấm công", description = "API cho chấm công")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(summary = "Check in")
    @PostMapping("/check-in")
    @CheckPermission(api = "/qlns/attendance/check-in", action = "CREATE")
    public ApiResponse<?> checkIn(@RequestBody AttendanceCheckInRequest request) {
        return ApiResponse.success(attendanceService.checkIn(request));
    }

    @Operation(summary = "Check out")
    @PostMapping("/check-out")
    @CheckPermission(api = "/qlns/attendance/check-out", action = "CREATE")
    public ApiResponse<?> checkOut(@RequestBody AttendanceCheckOutRequest request) {
        return ApiResponse.success(attendanceService.checkOut(request));
    }

    @Operation(summary = "Danh sách chấm công")
    @GetMapping
    @CheckPermission(api = "/qlns/attendance", action = "VIEW")
    public ApiResponse<?> all(@ModelAttribute AttendanceFilter filter) {
        return ApiResponse.success(attendanceService.all(filter));
    }

    @Operation(summary = "Roster chấm công toàn công ty theo ngày",
            description = "GET /qlns/attendance/daily?date=YYYY-MM-DD&departmentId=&status=&pageNumber=&pageSize. "
                    + "Left-join NV active × attendance(date) — thiếu record = NOT_CHECKED_IN. "
                    + "displayStatus: OK | LATE | NOT_CHECKED_IN | CHECKED_OUT | … Filter status/dept/paging.")
    @GetMapping("/daily")
    @CheckPermission(api = "/qlns/attendance/daily", action = "VIEW")
    public ApiResponse<?> daily(@ModelAttribute AttendanceFilter filter) {
        return ApiResponse.success(attendanceService.daily(filter));
    }

    @Operation(summary = "KPI tháng cho Home dashboard Mobile",
               description = "Trả về workingDays, presentDays, lateDays, tổng phút OT, số phép còn lại...")
    @GetMapping("/stats")
    @CheckPermission(api = "/qlns/attendance/stats", action = "VIEW")
    public ApiResponse<?> stats(@RequestParam String personId,
                                @RequestParam(required = false) String contractId,
                                @RequestParam(required = false) Integer month,
                                @RequestParam(required = false) Integer year) {
        return ApiResponse.success(attendanceService.getStats(personId, contractId, month, year));
    }

    @Operation(summary = "Chi tiết chấm công")
    @GetMapping("/{id}")
    @CheckPermission(api = "/qlns/attendance/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(attendanceService.getById(id));
    }
}
