package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.LeaveRequestAddRequest;
import com.frezo.qlns.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qlns/leave-request")
@RequiredArgsConstructor
@Tag(name = "Quản lý nghỉ phép", description = "API cho xin nghỉ phép")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @Operation(summary = "Tạo đơn xin nghỉ phép")
    @PostMapping
    public ApiResponse<?> create(@RequestBody LeaveRequestAddRequest request) {
        return ApiResponse.success(leaveRequestService.create(request));
    }

    @Operation(summary = "Danh sách đơn nghỉ phép của tôi")
    @GetMapping("/my/{contractId}")
    public ApiResponse<?> getMyReqs(@PathVariable String contractId) {
        return ApiResponse.success(leaveRequestService.getMyRequests(contractId));
    }

    @Operation(summary = "Danh sách đơn chờ duyệt")
    @GetMapping("/pending")
    public ApiResponse<?> pending(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(leaveRequestService.allPending(page, size));
    }

    @Operation(summary = "Duyệt đơn")
    @PutMapping("/{id}/approve")
    public ApiResponse<?> approve(@PathVariable String id) {
        return ApiResponse.success(leaveRequestService.approve(id));
    }

    @Operation(summary = "Từ chối đơn")
    @PutMapping("/{id}/reject")
    public ApiResponse<?> reject(@PathVariable String id, @RequestParam String reason) {
        return ApiResponse.success(leaveRequestService.reject(id, reason));
    }
}
