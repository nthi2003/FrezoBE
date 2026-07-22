package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.RequisitionRequest;
import com.frezo.qlns.dto.response.RequisitionResponse;
import com.frezo.qlns.service.RequisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/qlns/recruitment/requisitions")
@RequiredArgsConstructor
@Tag(name = "Recruitment - Requisition", description = "Quản lý nhu cầu tuyển dụng")
public class RequisitionController {

    private final RequisitionService requisitionService;

    @Operation(summary = "Tạo mới nhu cầu tuyển dụng")
    @PostMapping
    public ApiResponse<RequisitionResponse> create(@RequestBody RequisitionRequest req) {
        return ApiResponse.ok(requisitionService.create(req));
    }

    @Operation(summary = "Danh sách nhu cầu tuyển dụng (lọc theo status)")
    @GetMapping
    public ApiResponse<List<RequisitionResponse>> list(@RequestParam(required = false) String status) {
        return ApiResponse.ok(requisitionService.list(status));
    }

    @Operation(summary = "Chi tiết 1 nhu cầu tuyển dụng")
    @GetMapping("/{id}")
    public ApiResponse<RequisitionResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(requisitionService.getById(id));
    }

    @Operation(summary = "Cập nhật nhu cầu tuyển dụng")
    @PutMapping("/{id}")
    public ApiResponse<RequisitionResponse> update(@PathVariable String id,
                                                   @RequestBody RequisitionRequest req) {
        return ApiResponse.ok(requisitionService.update(id, req));
    }

    @Operation(summary = "Đóng nhu cầu tuyển dụng (không cho apply mới)")
    @PostMapping("/{id}/close")
    public ApiResponse<RequisitionResponse> close(@PathVariable String id) {
        return ApiResponse.ok(requisitionService.close(id));
    }
}
