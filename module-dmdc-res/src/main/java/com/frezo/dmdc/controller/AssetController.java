package com.frezo.dmdc.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtbv.dto.request.CategoryFilter;
import com.frezo.qtbv.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qlts/assets")
@RequiredArgsConstructor
@Tag(name = "12. Quản lý tài sản (QLTS)", description = "API quản lý tài sản, dựa trên danh mục với groupCode = 'QLTS'")
public class AssetController {

    private final CategoryService categoryService;

    @Operation(summary = "Lấy danh sách tài sản", description = "Thực chất là lấy danh mục với groupCode='QLTS'")
    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(CategoryFilter filter) {
        if (filter == null) filter = new CategoryFilter();
        filter.setType("QLTS"); // Force filter by QLTS group
        return ResponseEntity.ok(ApiResponse.success(categoryService.all(filter)));
    }
}
