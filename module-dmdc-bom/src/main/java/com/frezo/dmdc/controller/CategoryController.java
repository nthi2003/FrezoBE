package com.frezo.dmdc.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtbv.dto.request.CategoryFilter;
import com.frezo.qtbv.dto.request.CategoryRequest;
import com.frezo.qtbv.service.CategoryService;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qtht/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            summary = "Lấy danh sách danh mục",
            description = """
                    Lọc theo groupCode (ưu tiên) hoặc type (alias cùng cột group_code).
                    Canonical chức danh: GET /qtht/category?groupCode=ChucDanh (pageNumber/pageSize).
                    Legacy TITLE đã migrate → ChucDanh (seed category_data.sql).
                    """
    )
    @GetMapping
    @CheckPermission(api = "/qtht/category", action = "VIEW")
    public ResponseEntity<ApiResponse<?>> getAll (CategoryFilter filter) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.all(filter)));
    }

    @Operation(summary = "Tạo mới danh mục", description = "Tạo mới danh mục")
    @PostMapping
    @CheckPermission(api = "/qtht/category", action = "CREATE")
    public ResponseEntity<ApiResponse<?>> add (@RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.add(request));
    }
    @Operation(summary = "Chỉnh sửa danh mục", description = "Chỉnh sửa mới danh mục")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qtht/category/{id}", action = "UPDATE")
    public ResponseEntity<ApiResponse<?>> edit (@PathVariable("id") String id , @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.edit(id, request));
    }
    @Operation(summary = "Delete danh mục")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qtht/category/{id}", action = "DELETE")
    public void delete(@PathVariable("id") String id) {
        categoryService.delete(id);
    }
    @Operation(summary = "Xem chi tiết danh mục")
    @GetMapping("/{id}")
    @CheckPermission(api = "/qtht/category/{id}", action = "VIEW")
    public ResponseEntity<ApiResponse<?>> view(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.view(id)));
    }


}
