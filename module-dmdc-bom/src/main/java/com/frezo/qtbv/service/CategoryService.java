package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.CategoryFilter;
import com.frezo.qtbv.dto.request.CategoryRequest;
import com.frezo.qtbv.dto.response.CategoryResponse;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;

public interface CategoryService  {
    PageResponse<CategoryResponse> all(CategoryFilter filter);
    ApiResponse<?> add(CategoryRequest request);
    ApiResponse<?> edit(String id, CategoryRequest request);
    CategoryResponse view (String id);
    void delete (String id);
}
