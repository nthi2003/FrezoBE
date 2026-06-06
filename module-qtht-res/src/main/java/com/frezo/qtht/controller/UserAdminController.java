package com.frezo.qtht.controller;

import com.frezo.auth.dto.request.RegisterRequest;
import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.config.CheckPermission;
import com.frezo.qtht.dto.response.UserResponse;
import com.frezo.qtht.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "4. Quản lý Người dùng (Users)", description = "Các API đăng ký tài khoản và thiết lập vai trò cho người dùng")
public class UserAdminController {

    private final UserAdminService userAdminService;

    @Operation(summary = "Đăng ký tài khoản mới", description = "Tạo tài khoản người dùng mới vào hệ thống. Bao gồm tạo User, Person và gán Role mặc định. Không cần authentication.")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        userAdminService.register(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng ký tài khoản thành công"));
    }

    @Operation(summary = "Gán Role cho Người dùng", description = "Cấp quyền (Role) cho một người dùng cụ thể dựa trên appCode. Ví dụ: appCode = 'QTHT'")
    @PostMapping("/assign-role")
    @CheckPermission(api = "/users/assign-role", action = "CREATE")
    public ResponseEntity<ApiResponse<String>> assignRole(
            @RequestParam("username") String username,
            @RequestParam("roleCode") String roleCode,
            @RequestParam("appCode") String appCode) {
        userAdminService.assignRole(username, roleCode, appCode);
        return ResponseEntity.ok(ApiResponse.success("Role assigned successfully"));
    }

    @Operation(summary = "Lấy danh sách Role của người dùng", description = "Trả về danh sách các mã Role (String) mà người dùng đang sở hữu")
    @GetMapping("/{username}/roles")
    @CheckPermission(api = "/users/{username}/roles", action = "VIEW")
    public ResponseEntity<ApiResponse<List<String>>> getUserRoles(@PathVariable("username") String username) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.getRoles(username)));
    }

    @Operation(summary = "Lấy danh sách tất cả người dùng", description = "Lấy danh sách người dùng với phân trang và tìm kiếm")
    @GetMapping("/all")
    @CheckPermission(api = "/users/all", action = "VIEW")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search) {
        Map<String, Object> result = userAdminService.getAllUsers(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Lấy thông tin chi tiết người dùng", description = "Lấy thông tin chi tiết của một người dùng theo ID")
    @GetMapping("/{id}")
    @CheckPermission(api = "/users/{id}", action = "VIEW")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(userAdminService.getUserById(id)));
    }
    @Operation(summary = "Bật trạng thái hoạt động", description = "Mở khóa tài khoản người dùng")
    @PutMapping("/{id}/active")
    @CheckPermission(api = "/users/{id}/active", action = "UPDATE")
    public ResponseEntity<ApiResponse<String>> active(@PathVariable("id") String id) {
        userAdminService.actived(id);
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã được mở khóa thành công"));
    }

    @Operation(summary = "Khóa tài khoản", description = "Khóa tài khoản người dùng (set status = 0)")
    @PutMapping("/{id}/lock")
    @CheckPermission(api = "/users/{id}/lock", action = "UPDATE")
    public ResponseEntity<ApiResponse<String>> inactive(@PathVariable("id") String id) {
        userAdminService.inactived(id);
        return ResponseEntity.ok(ApiResponse.success("Tài khoản đã bị khóa thành công"));
    }

    @Operation(summary = "Reset mật khẩu", description = "Reset mật khẩu người dùng về mặc định (123456)")
    @PostMapping("/{id}/reset-password")
    @CheckPermission(api = "/users/{id}/reset-password", action = "UPDATE")
    public ResponseEntity<ApiResponse<String>> resetPassword(@PathVariable("id") String id) {
        String newPass = userAdminService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success(newPass, "Mật khẩu đã được reset về mặc định"));
    }
}
