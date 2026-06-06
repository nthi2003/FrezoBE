package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
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
    public ApiResponse<?> checkIn(@RequestBody AttendanceCheckInRequest request) {
        return ApiResponse.success(attendanceService.checkIn(request));
    }

    @Operation(summary = "Check out")
    @PostMapping("/check-out")
    public ApiResponse<?> checkOut(@RequestBody AttendanceCheckOutRequest request) {
        return ApiResponse.success(attendanceService.checkOut(request));
    }

    @Operation(summary = "Danh sách chấm công")
    @GetMapping
    public ApiResponse<?> all(@ModelAttribute AttendanceFilter filter) {
        return ApiResponse.success(attendanceService.all(filter));
    }

    @Operation(summary = "Chi tiết chấm công")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(attendanceService.getById(id));
    }
}
