package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.common.response.PageResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qtht.dto.request.DepartmentFilterRequest;
import com.frezo.qtht.dto.request.DepartmentSaveRequest;
import com.frezo.qtht.dto.response.DepartmentResponse;
import com.frezo.qtht.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Department controller — <b>reference implementation cho Batch F</b> (chuẩn v1.1).
 * <p>
 * Combobox/tree: vẫn yêu cầu VIEW (dùng chung form khác module; role cần seed VIEW).
 * SUPER_ADMIN bypass qua {@code Person.isAdmin=true}.
 */
@RestController
@RequestMapping("/qtht/department")
@RequiredArgsConstructor
@Tag(name = "DepartmentController", description = "Quản lý phòng ban")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng ban (có tìm kiếm & lọc + phân trang)")
    @CheckPermission(api = "/qtht/department", action = "VIEW")
    public ApiResponse<PageResponse<DepartmentResponse>> getAllDepartments(
            @ModelAttribute @Valid DepartmentFilterRequest request) {
        return ApiResponse.ok(departmentService.all(request));
    }

    @GetMapping("/tree")
    @Operation(summary = "Lấy cây phòng ban (phân cấp cha con)")
    @CheckPermission(api = "/qtht/department/tree", action = "VIEW")
    public ApiResponse<List<DepartmentResponse>> getTree() {
        return ApiResponse.ok(departmentService.getTree());
    }

    @GetMapping("/combobox")
    @Operation(summary = "Danh sách phòng ban dạng combobox — lookup dùng chung, JWT only")
    public ApiResponse<List<ComboboxResponse>> getCombobox() {
        return ApiResponse.ok(departmentService.getCombobox());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phòng ban (soft-delete)")
    @CheckPermission(api = "/qtht/department/{id}", action = "DELETE")
    public ApiResponse<Void> deleteDepartment(@PathVariable String id) {
        departmentService.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping
    @Operation(summary = "Thêm mới phòng ban")
    @CheckPermission(api = "/qtht/department", action = "CREATE")
    public ApiResponse<DepartmentResponse> createDepartment(
            @RequestBody @Valid DepartmentSaveRequest request) {
        return ApiResponse.created(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phòng ban")
    @CheckPermission(api = "/qtht/department/{id}", action = "UPDATE")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable String id,
            @RequestBody @Valid DepartmentSaveRequest request) {
        return ApiResponse.ok(departmentService.update(id, request));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Kích hoạt phòng ban")
    @CheckPermission(api = "/qtht/department/{id}/activate", action = "UPDATE")
    public ApiResponse<Void> activateDepartment(@PathVariable String id) {
        departmentService.activate(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Vô hiệu hóa phòng ban")
    @CheckPermission(api = "/qtht/department/{id}/deactivate", action = "UPDATE")
    public ApiResponse<Void> deactivateDepartment(@PathVariable String id) {
        departmentService.deactivate(id);
        return ApiResponse.ok();
    }
}
