package com.frezo.customer.controller;

import com.frezo.customer.dto.request.CustomerFilterRequest;
import com.frezo.customer.dto.request.CustomerRequest;
import com.frezo.customer.service.CustomerService;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@Tag(name = "Quản lý Khách hàng", description = "CRUD, import/export Excel, bảo mật SĐT")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Danh sách khách hàng (có lọc, phân trang)")
    @GetMapping
    public ApiResponse<?> getAll(@ModelAttribute CustomerFilterRequest filter) {
        return ApiResponse.ok(customerService.getAll(filter));
    }

    @Operation(summary = "Chi tiết khách hàng")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.ok(customerService.getById(id));
    }

    @Operation(summary = "Tạo mới khách hàng")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @Operation(summary = "Cập nhật khách hàng")
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id,
                               @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.update(id, request));
    }

    @Operation(summary = "Xóa khách hàng")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        customerService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Xem SĐT thật (audit log, yêu cầu quyền ADMIN)")
    @GetMapping("/{id}/reveal-phone")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CUSTOMER_REVEAL_PHONE')")
    public ApiResponse<?> revealPhone(@PathVariable String id) {
        return ApiResponse.ok(customerService.revealPhone(id));
    }

    @Operation(summary = "Import khách hàng từ Excel")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> importExcel(@RequestParam("file") MultipartFile file) {
        customerService.importFromExcel(file);
        return ApiResponse.ok("Import thành công");
    }

    @Operation(summary = "Export khách hàng ra Excel")
    @GetMapping("/export")
    public void exportExcel(@ModelAttribute CustomerFilterRequest filter,
                            HttpServletResponse response) throws Exception {
        byte[] data = customerService.exportToExcel(filter);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=khach-hang.xlsx");
        response.getOutputStream().write(data);
    }
}
