package com.frezo.dmdc.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtbv.dto.request.CategoryFilter;
import com.frezo.qtbv.dto.request.CategoryRequest;
import com.frezo.qtbv.service.CategoryService;
import com.frezo.qtht.config.CheckPermission;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qlht/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(summary = "Lấy danh sách danh mục", description = "Lấy danh sách danh mục có phân trang và lọc")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll (CategoryFilter filter) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.all(filter)));
    }

    @Operation(summary = "Tạo mới danh mục", description = "Tạo mới danh mục")
    @PostMapping
    @CheckPermission(api = "/qtht/category", action = "CREATE")
    public ResponseEntity<ApiResponse<?>> add (@RequestBody CategoryRequest request) {
        Response<?> response = categoryService.add(request);
        return ResponseEntity.ok(ApiResponse.success(response.getData(), response.getMessage()));
    }
    @Operation(summary = "Chỉnh sửa danh mục", description = "Chỉnh sửa mới danh mục")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qtht/category", action = "EDIT")
    public ResponseEntity<ApiResponse<?>> edit (@PathVariable("id") String id , @RequestBody CategoryRequest request) {
        Response<?> response = categoryService.edit(id,request);
        return ResponseEntity.ok(ApiResponse.success(response.getData(), response.getMessage()));
    }
    @Operation(summary = "Delete danh mục")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") String id) {
        categoryService.delete(id);
    }
    @Operation(summary = "Xem chi tiết danh mục")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> view(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.view(id)));
    }


}
