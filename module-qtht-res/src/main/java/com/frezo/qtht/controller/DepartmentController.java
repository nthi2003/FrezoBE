package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.dto.request.DepartmentFilterRequest;
import com.frezo.qtht.dto.request.DepartmentSaveRequest;
import com.frezo.qtht.service.DepartmentService;
import com.frezo.qtht.config.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
