package com.frezo.customer.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.customer.dto.request.NCCFilterRequest;
import com.frezo.customer.dto.request.NCCRequest;
import com.frezo.customer.service.NCCService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ncc")
@RequiredArgsConstructor
@Tag(name = "Quản lý Nhà cung cấp (NCC)", description = "Các API quản lý hồ sơ và năng lực nhà cung cấp rau củ")
public class NCCController {

    private final NCCService nccService;

    @Operation(summary = "Danh sách nhà cung cấp (có lọc)", description = "Lấy danh sách NCC dựa trên từ khóa hoặc phân loại")
    @GetMapping("/all")
    @CheckPermission(api = "/ncc/all", action = "VIEW")
    public ApiResponse<?> all(@ModelAttribute NCCFilterRequest filter) {
        return ApiResponse.success(nccService.all(filter));
    }

    @Operation(summary = "Chi tiết nhà cung cấp", description = "Lấy thông tin chi tiết NCC theo ID")
    @GetMapping("/{id}")
    @CheckPermission(api = "/ncc/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(nccService.getById(id));
    }

    @Operation(summary = "Tạo mới nhà cung cấp", description = "Thêm một NCC mới vào hệ thống")
    @PostMapping("")
    @CheckPermission(api = "/ncc", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody NCCRequest request) {
        return ApiResponse.success(nccService.createNCC(request));
    }

    @Operation(summary = "Cập nhật nhà cung cấp", description = "Cập nhật thông tin NCC theo ID")
    @PutMapping("/{id}")
    @CheckPermission(api = "/ncc/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody NCCRequest request) {
        return ApiResponse.success(nccService.updateNCC(id, request));
    }

    @Operation(summary = "Xóa nhà cung cấp", description = "Xóa NCC khỏi hệ thống")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/ncc/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        nccService.delete(id);
        return ApiResponse.success("Xóa thành công");
    }

    @PostMapping("/upload-certificate")
    @CheckPermission(api = "/ncc/upload-certificate", action = "CREATE")
    @Operation(summary = "Upload chứng chỉ NCC", description = "Tải file scan chứng chỉ lên MinIO")
    public ApiResponse<?> uploadCertificate(
            @RequestParam("nccCode") String nccCode,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        return ApiResponse.success(nccService.uploadCertificate(nccCode, file));
    }
}
