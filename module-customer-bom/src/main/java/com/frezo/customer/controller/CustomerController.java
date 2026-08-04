package com.frezo.customer.controller;

import com.frezo.customer.dto.request.CustomerFilterRequest;
import com.frezo.customer.dto.request.CustomerRequest;
import com.frezo.customer.service.CustomerService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
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
    @CheckPermission(api = "/customer", action = "VIEW")
    public ApiResponse<?> getAll(@ModelAttribute CustomerFilterRequest filter) {
        return ApiResponse.ok(customerService.getAll(filter));
    }

    @Operation(summary = "Chi tiết khách hàng")
    @GetMapping("/{id}")
    @CheckPermission(api = "/customer/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.ok(customerService.getById(id));
    }

    @Operation(summary = "Tạo mới khách hàng")
    @PostMapping
    @CheckPermission(api = "/customer", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @Operation(summary = "Cập nhật khách hàng")
    @PutMapping("/{id}")
    @CheckPermission(api = "/customer/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id,
                               @Valid @RequestBody CustomerRequest request) {
        return ApiResponse.ok(customerService.update(id, request));
    }

    @Operation(summary = "Xóa khách hàng")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/customer/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        customerService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Xem SĐT thật (audit log, yêu cầu quyền ADMIN)")
    @GetMapping("/{id}/reveal-phone")
    @CheckPermission(api = "/customer/{id}/reveal-phone", action = "VIEW")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('CUSTOMER_REVEAL_PHONE')")
    public ApiResponse<?> revealPhone(@PathVariable String id) {
        return ApiResponse.ok(customerService.revealPhone(id));
    }

    @Operation(summary = "Import khách hàng từ Excel")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CheckPermission(api = "/customer/import", action = "UPDATE")
    public ApiResponse<?> importExcel(@RequestParam("file") MultipartFile file) {
        customerService.importFromExcel(file);
        return ApiResponse.ok("Import thành công");
    }

    @Operation(summary = "Export khách hàng ra Excel")
    @GetMapping("/export")
    @CheckPermission(api = "/customer/export", action = "VIEW")
    public void exportExcel(@ModelAttribute CustomerFilterRequest filter,
                            HttpServletResponse response) throws Exception {
        byte[] data = customerService.exportToExcel(filter);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=khach-hang.xlsx");
        response.getOutputStream().write(data);
    }

    @Operation(summary = "Upload / cập nhật avatar khách hàng")
    @PostMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CheckPermission(api = "/customer/{id}/avatar", action = "CREATE")
    public ApiResponse<?> uploadAvatar(@PathVariable String id,
                                       @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(customerService.uploadAvatar(id, file));
    }
}
