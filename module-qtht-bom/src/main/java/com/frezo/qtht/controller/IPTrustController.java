package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;
import com.frezo.qtht.dto.request.IpTrustAddRequest;
import com.frezo.qtht.dto.request.IpTrustEditRequest;
import com.frezo.qtht.dto.request.IpTrustFilter;
import com.frezo.qtht.dto.response.IpTrustResponse;
import com.frezo.qtht.service.IPTrustService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qtht/ip-trust")
@RequiredArgsConstructor
@Tag(name = "Quản lí danh sách white list ip", description = "Các API cho phép quản lí danh sách white list IP đặc biệt được phép thông qua")
public class IPTrustController {

    private final IPTrustService ipTrustService;

    @Operation(summary = "Lấy danh sách white list ip", description = "Lấy tất cả các ip trong danh sách trắng với phân trang và tìm kiếm theo keyword")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<IpTrustResponse>>> all(IpTrustFilter filter) {
        return ResponseEntity.ok(ApiResponse.success(ipTrustService.all(filter)));
    }

    @Operation(summary = "Thêm mới ip vào white list", description = "Thêm mới một địa chỉ IP cùng tên mô tả vào hệ thống")
    @PostMapping
    public ResponseEntity<ApiResponse<IpTrustResponse>> add(@Valid @RequestBody IpTrustAddRequest request) {
        return ResponseEntity.ok(ipTrustService.add(request));
    }

    @Operation(summary = "Chỉnh sửa thông tin ip", description = "Cập nhật tên hoặc số IP hiện có trong danh sách")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IpTrustResponse>> edit(
            @PathVariable String id,
            @Valid @RequestBody IpTrustEditRequest request) {
        return ResponseEntity.ok(ipTrustService.edit(id, request));
    }

    @Operation(summary = "Chi tiết white list ip", description = "Lấy thông tin chi tiết một IP dựa trên ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IpTrustResponse>> view(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(ipTrustService.view(id)));
    }

    @Operation(summary = "Xóa ip khỏi white list", description = "Thực hiện xóa (soft delete) địa chỉ IP khỏi danh sách trắng")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable String id) {
        return ResponseEntity.ok(ipTrustService.delete(id));
    }
}
