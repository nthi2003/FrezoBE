package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.config.CheckPermission;
import com.frezo.qtht.dto.request.RolePermissionSaveRequest;
import com.frezo.qtht.service.RoleMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/qtht/role-menu")
@RequiredArgsConstructor
@Tag(name = "5. Phân quyền Menu (Role-Menu)", description = "Các API thiết lập Menu(permission) nào được xuất hiện cho Role nào")
public class RoleMenuController {

    private final RoleMenuService roleMenuService;


    @Operation(summary = "Lấy danh sách Menu theo Role", description = "Trả về tất cả các bản ghi RoleMenu của một Role cụ thể")
    @GetMapping("/role/{roleCode}")
    //    @CheckPermission(api = "/qtht/role-menu/role/{roleCode}", action = "VIEW")
    public ResponseEntity<ApiResponse<List<?>>> getMenusByRole(@PathVariable String roleCode) {
        log.debug("Fetching menus for role: {}", roleCode);
        List<?> menus = roleMenuService.getMenusByRole(roleCode);
        log.debug("Found {} menus for role: {}", menus.size(), roleCode);
        return ResponseEntity.ok(ApiResponse.success(menus));
    }

    @Operation(summary = "Lưu tất cả quyền cho một vai trò", description = "Xóa toàn bộ quyền cũ và ghi đè quyền mới dựa trên danh sách tick chọn từ UI")
    @PostMapping("/save-all")
    // @CheckPermission(api = "/qtht/role-menu/save-all", action = "UPDATE")
    public ResponseEntity<ApiResponse<String>> saveAllPermissions(@RequestBody RolePermissionSaveRequest request) {
        try {
            log.debug("Saving permissions for role: {}, appCode: {}, menuIds count: {}",
                    request.getRoleId(),
                    request.getAppCode(),
                    request.getMenuIds() != null ? request.getMenuIds().size() : 0);

            roleMenuService.savePermissions(request);
            return ResponseEntity.ok(ApiResponse.success("Lưu phân quyền thành công"));

        } catch (Exception e) {
            log.error("Error saving permissions for role: {}", request.getRoleId(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(500, "Lỗi Server: " + e.getMessage()));
        }
    }
}
