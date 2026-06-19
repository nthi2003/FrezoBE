package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.qtht.dto.request.DepartmentFilterRequest;
import com.frezo.qtht.dto.request.DepartmentSaveRequest;
import com.frezo.qtht.dto.response.DepartmentResponse;
import com.frezo.qtht.service.DepartmentService;
import com.frezo.qtht.config.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qtht/department")
@RequiredArgsConstructor
@Tag(name = "DepartmentController", description = "Quản lý phòng ban")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    @Operation(summary = "Lấy danh sách phòng ban (có tìm kiếm & lọc)")
////    @CheckPermission(api = "/qtht/department", action = "VIEW")
    public ApiResponse<Map<String, Object>> getAllDepartments(@ModelAttribute @Valid DepartmentFilterRequest request) {
        return ApiResponse.success(departmentService.all(request));
    }

    @GetMapping("/tree")
    @Operation(summary = "Lấy cây phòng ban (phân cấp cha con)")
    public ApiResponse<List<DepartmentResponse>> getTree() {
        return ApiResponse.success(departmentService.getTree());
    }

    @GetMapping("/combobox")
    @Operation(summary = "Danh sách phòng ban dạng combobox")
    public ApiResponse<List<ComboboxResponse>> getCombobox() {
        DepartmentFilterRequest filter = new DepartmentFilterRequest();
        filter.setPageNumber(0);
        filter.setPageSize(Integer.MAX_VALUE);
        Map<String, Object> data = departmentService.all(filter);
        List<DepartmentResponse> items = (List<DepartmentResponse>) data.get("items");
        List<ComboboxResponse> result = items.stream()
                .map(d -> ComboboxResponse.builder()
                        .value(d.getId())
                        .label(d.getName() + " (" + d.getCode() + ")")
                        .build())
                .toList();
        return ApiResponse.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa phòng ban")
////    @CheckPermission(api = "/qtht/department", action = "DELETE")
    public ApiResponse<Void> deleteDepartment(@PathVariable String id) {
        departmentService.delete(id);
        return ApiResponse.success(null, "Xóa phòng ban thành công");
    }

    @PostMapping
    @Operation(summary = "Thêm mới phòng ban")
////    @CheckPermission(api = "/qtht/department", action = "CREATE")
    public ApiResponse<Object> createDepartment(@RequestBody @Valid DepartmentSaveRequest request) {
        return ApiResponse.success(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật phòng ban")
////    @CheckPermission(api = "/qtht/department", action = "UPDATE")
    public ApiResponse<Object> updateDepartment(@PathVariable String id,
            @RequestBody @Valid DepartmentSaveRequest request) {
        return ApiResponse.success(departmentService.update(id, request));
    }

    @PutMapping("/{id}/activate")
    @Operation(summary = "Kích hoạt phòng ban")
    public ApiResponse<Void> activateDepartment(@PathVariable String id) {
        departmentService.activate(id);
        return ApiResponse.success(null, "Kích hoạt phòng ban thành công");
    }

    @PutMapping("/{id}/deactivate")
    @Operation(summary = "Vô hiệu hóa phòng ban")
    public ApiResponse<Void> deactivateDepartment(@PathVariable String id) {
        departmentService.deactivate(id);
        return ApiResponse.success(null, "Vô hiệu hóa phòng ban thành công");
    }
}
