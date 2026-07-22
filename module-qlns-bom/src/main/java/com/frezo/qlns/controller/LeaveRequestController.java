package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.LeaveRequestAddRequest;
import com.frezo.qlns.service.LeaveRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/qlns/leave-request")
@RequiredArgsConstructor
@Tag(name = "Quản lý nghỉ phép", description = "Workflow xin — duyệt đơn nghỉ phép 2 tầng (QL trực tiếp → HR)")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @Operation(summary = "Tạo đơn xin nghỉ phép")
    @PostMapping
    public ApiResponse<?> create(@RequestBody LeaveRequestAddRequest request) {
        return ApiResponse.success(leaveRequestService.create(request));
    }

    @Operation(summary = "Danh sách đơn nghỉ phép của tôi (mọi trạng thái, mới nhất trước)")
    @GetMapping("/my/{contractId}")
    public ApiResponse<?> getMyReqs(@PathVariable String contractId) {
        return ApiResponse.success(leaveRequestService.getMyRequests(contractId));
    }

    @Operation(summary = "Danh sách đơn cần tôi duyệt (server tự lọc theo role)")
    @GetMapping("/pending")
    public ApiResponse<?> pending(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(leaveRequestService.allPending(page, size));
    }

    @Operation(summary = "Duyệt đơn — DEPRECATED 410, dùng POST /approvals/{id}/approve")
    @PutMapping("/{id}/approve")
    public ApiResponse<?> approve(@PathVariable String id) {
        return ApiResponse.success(leaveRequestService.approve(id));
    }

    @Operation(summary = "Từ chối đơn — DEPRECATED 410, dùng POST /approvals/{id}/reject")
    @PutMapping("/{id}/reject")
    public ApiResponse<?> reject(@PathVariable String id, @RequestBody Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return ApiResponse.success(leaveRequestService.reject(id, reason));
    }

    @Operation(summary = "Huỷ đơn — chỉ requester (khi còn PENDING) hoặc admin")
    @PutMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable String id) {
        return ApiResponse.success(leaveRequestService.cancel(id));
    }

    @Operation(summary = "Timeline audit trail của đơn")
    @GetMapping("/{id}/history")
    public ApiResponse<?> history(@PathVariable String id) {
        return ApiResponse.success(leaveRequestService.getHistory(id));
    }
}
