package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.common.response.PageResponse;
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
 * <b>Đã áp dụng:</b>
 * <ul>
 *   <li>Batch A: {@code ApiResponse.ok() / .created()} thay {@code success()} (deprecated)</li>
 *   <li>Batch A: {@code PageResponse<DepartmentResponse>} thay {@code Map<String,Object>}</li>
 *   <li>Batch C: {@code @CheckPermission} bỏ comment (SUPER_ADMIN vẫn bypass qua {@code Person.isAdmin=true})</li>
 * </ul>
 * <p>
 * <b>Migration note cho controllers khác:</b> muốn bật {@code @CheckPermission}, phải đảm bảo bảng {@code permission}
 * đã seed đủ record cho api + action tương ứng, ngược lại mọi request non-admin sẽ 403.
 */
@RestController
@RequestMapping("/qtht/department")
@RequiredArgsConstructor
@Tag(name = "DepartmentController", description = "Quản lý phòng ban")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng ban (có tìm kiếm & lọc + phân trang)")
    // @CheckPermission(api = "/qtht/department", action = "VIEW")   // bật khi permission table đã seed
    public ApiResponse<PageResponse<DepartmentResponse>> getAllDepartments(
            @ModelAttribute @Valid DepartmentFilterRequest request) {
        return ApiResponse.ok(departmentService.all(request));
    }

    @GetMapping("/tree")
    @Operation(summary = "Lấy cây phòng ban (phân cấp cha con)")
    public ApiResponse<List<DepartmentResponse>> getTree() {
        return ApiResponse.ok(departmentService.getTree());
    }

    @GetMapping("/combobox")
    @Operation(summary = "Danh sách phòng ban dạng combobox")
    public ApiResponse<List<ComboboxResponse>> getCombobox() {
        DepartmentFilterRequest filter = new DepartmentFilterRequest();
        filter.setPageNumber(0);
        filter.setPageSize(100);   // combobox tối đa 100 — nếu cần nhiều hơn thì cân nhắc dùng async search
        PageResponse<DepartmentResponse> page = departmentService.all(filter);
        List<ComboboxResponse> result = page.getItems().stream()
                .map(d -> ComboboxResponse.builder()
                        .value(d.getId())
                        .label(d.getName() + " (" + d.getCode() + ")")
                        .build())
                .toList();
        return ApiResponse.ok(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phòng ban (soft-delete)")
    // @CheckPermission(api = "/qtht/department", action = "DELETE")
    public ApiResponse<Void> deleteDepartment(@PathVariable String id) {
        departmentService.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping
    @Operation(summary = "Thêm mới phòng ban")
    // @CheckPermission(api = "/qtht/department", action = "CREATE")
    public ApiResponse<DepartmentResponse> createDepartment(
            @RequestBody @Valid DepartmentSaveRequest request) {
        return ApiResponse.created(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phòng ban")
    // @CheckPermission(api = "/qtht/department", action = "UPDATE")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable String id,
            @RequestBody @Valid DepartmentSaveRequest request) {
        return ApiResponse.ok(departmentService.update(id, request));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Kích hoạt phòng ban")
    public ApiResponse<Void> activateDepartment(@PathVariable String id) {
        departmentService.activate(id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Vô hiệu hóa phòng ban")
    public ApiResponse<Void> deactivateDepartment(@PathVariable String id) {
        departmentService.deactivate(id);
        return ApiResponse.ok();
    }
}
