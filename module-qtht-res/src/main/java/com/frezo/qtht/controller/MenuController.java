package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.dto.request.MenuSaveRequest;
import com.frezo.qtht.dto.response.MenuResponse;
import com.frezo.qtht.mapper.MenuMapper;
import com.frezo.qtht.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qtht/menu")
@RequiredArgsConstructor
@Tag(name = "3. Quản lý Menu (QTHT)", description = "Các API quản lý sơ đồ menu, phân quyền hiển thị theo người dùng")
public class MenuController {

    private final MenuService menuService;
    private final MenuMapper menuMapper;

    @Operation(summary = "Lấy menu theo người dùng", description = "Nhận username và trả về danh sách menu(permission) mà người dùng cụ thể được phép nhìn thấy")
    @GetMapping("/user/{username}")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getMenusForUser(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(menuService.getMenusForUser(username)));
    }

    @Operation(summary = "Lấy tất cả menu", description = "Lấy toàn bộ cấu trúc menu có trong hệ thống")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(menuService.getAllMenus()));
    }

    @Operation(summary = "Chi tiết menu", description = "Lấy thông tin chi tiết của một menu dựa trên ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(menuMapper.toResponse(menuService.getById(id))));
    }

    @Operation(summary = "Tạo mới menu", description = "Tạo một menu mới. Lưu ý: appCode trong request nên để là 'QTHT' nếu thuộc module quản trị hệ thống.")
    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(@Valid @RequestBody MenuSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(menuService.create(request)));
    }

    @Operation(summary = "Cập nhật menu", description = "Chỉnh sửa thông tin menu hiện có")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody MenuSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(menuService.update(id, request)));
    }

    @Operation(summary = "Xóa menu", description = "Xóa vĩnh viễn một menu khỏi hệ thống (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable String id) {
        menuService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Menu deleted successfully"));
    }
}
