package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.qtht.dto.request.RoleCreateRequest;
import com.frezo.qtht.dto.request.RoleUpdateRequest;
import com.frezo.qtht.dto.response.RoleResponse;
import com.frezo.qtht.entity.Role;
import com.frezo.qtht.service.RoleService;
import com.frezo.qtht.config.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlht/roles")
@RequiredArgsConstructor
@Tag(name = "2. Quản lý Role", description = "Các API dành cho quản trị viên thiết lập quyền hạn")
public class RoleController {
    private final RoleService roleService;

    @Operation(summary = "Tạo mới Role", description = "Tạo một vai trò mới cho hệ thống dựa trên appCode")
    @PostMapping
    @CheckPermission(api = "/qlht/roles", action = "CREATE")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@RequestBody RoleCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(roleService.create(request)));
    }

    @Operation(summary = "Cập nhật Role", description = "Chỉnh sửa thông tin vai trò hiện có")
    @PutMapping
    @CheckPermission(api = "/qlht/roles", action = "UPDATE")
    public ResponseEntity<ApiResponse<RoleResponse>> update(@RequestBody RoleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(roleService.update(request)));
    }

    @Operation(summary = "Xóa Role (soft delete)")
    @DeleteMapping
    @CheckPermission(api = "/qlht/roles", action = "DELETE")
    public ResponseEntity<Void> delete(
            @RequestParam("code") String code,
            @RequestParam("appCode") String appCode) {
        roleService.delete(code, appCode);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lấy danh sách Role", description = "Lấy toàn bộ danh sách Role, có thể lọc theo appCode")
    @GetMapping
    @CheckPermission(api = "/qlht/roles", action = "VIEW")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(
            @RequestParam(name = "appCode", required = false) String appCode) {
        if (appCode != null) {
            return ResponseEntity.ok(ApiResponse.success(roleService.getRoleByAppCode(appCode)));
        }
        return ResponseEntity.ok(ApiResponse.success(roleService.getAll()));

    }

    @GetMapping("/combobox")
    public ResponseEntity<ApiResponse<List<ComboboxResponse>>> getCombobox(
            @RequestParam(name = "appCode", required = false) String appCode) {
        List<RoleResponse> roles;
        if (appCode != null) {
            roles = roleService.getRoleByAppCode(appCode);
        } else {
            roles = roleService.getAll();
        }

        List<ComboboxResponse> response = roles.stream()
                .map(r -> ComboboxResponse.builder()
                        .value(r.getCode())
                        .label(r.getName())
                        .description(r.getCode())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
