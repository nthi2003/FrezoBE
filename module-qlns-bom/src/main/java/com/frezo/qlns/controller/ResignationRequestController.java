package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.ResignationApproveRequest;
import com.frezo.qlns.dto.request.ResignationCreateRequest;
import com.frezo.qlns.dto.request.ResignationHandoverRequest;
import com.frezo.qlns.dto.response.ResignationResponse;
import com.frezo.qlns.service.ResignationRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/qlns/resignation")
@RequiredArgsConstructor
@Tag(name = "QLNS — Offboarding", description = "Đơn nghỉ việc — wizard 5 bước")
public class ResignationRequestController {

    private final ResignationRequestService resignationService;

    @Operation(summary = "Tạo đề xuất nghỉ việc")
    @PostMapping
    @CheckPermission(api = "/qlns/resignation", action = "CREATE")
    public ApiResponse<ResignationResponse> create(@RequestBody ResignationCreateRequest request) {
        return ApiResponse.ok(resignationService.create(request));
    }

    @Operation(summary = "Danh sách đơn nghỉ việc")
    @GetMapping
    @CheckPermission(api = "/qlns/resignation", action = "VIEW")
    public ApiResponse<List<ResignationResponse>> list(
            @RequestParam(required = false) String personId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(resignationService.list(personId, status));
    }

    @Operation(summary = "Chi tiết đơn nghỉ việc")
    @GetMapping("/{id}")
    @CheckPermission(api = "/qlns/resignation/{id}", action = "VIEW")
    public ApiResponse<ResignationResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(resignationService.getById(id));
    }

    @Operation(summary = "Bước 2 — Duyệt timeline & ngày làm việc cuối")
    @PostMapping("/{id}/approve")
    @CheckPermission(api = "/qlns/resignation/{id}/approve", action = "UPDATE")
    public ApiResponse<ResignationResponse> approve(
            @PathVariable String id,
            @RequestBody(required = false) ResignationApproveRequest request) {
        return ApiResponse.ok(resignationService.approve(id, request));
    }

    @Operation(summary = "Bước 3 — Xác nhận bàn giao tài sản")
    @PostMapping("/{id}/handover")
    @CheckPermission(api = "/qlns/resignation/{id}/handover", action = "CREATE")
    public ApiResponse<ResignationResponse> handover(
            @PathVariable String id,
            @RequestBody ResignationHandoverRequest request) {
        return ApiResponse.ok(resignationService.confirmHandover(id, request));
    }

    @Operation(summary = "Bước 4 — Chốt lương tháng cuối")
    @PostMapping("/{id}/settle-payroll")
    @CheckPermission(api = "/qlns/resignation/{id}/settle-payroll", action = "CREATE")
    public ApiResponse<ResignationResponse> settlePayroll(@PathVariable String id) {
        return ApiResponse.ok(resignationService.settlePayroll(id));
    }

    @Operation(summary = "Bước 5 — Thu hồi TK & hoàn tất offboarding")
    @PostMapping("/{id}/complete")
    @CheckPermission(api = "/qlns/resignation/{id}/complete", action = "UPDATE")
    public ApiResponse<ResignationResponse> complete(@PathVariable String id) {
        return ApiResponse.ok(resignationService.complete(id));
    }

    @Operation(summary = "Huỷ đơn nghỉ việc")
    @PostMapping("/{id}/cancel")
    @CheckPermission(api = "/qlns/resignation/{id}/cancel", action = "UPDATE")
    public ApiResponse<ResignationResponse> cancel(@PathVariable String id) {
        return ApiResponse.ok(resignationService.cancel(id));
    }
}
