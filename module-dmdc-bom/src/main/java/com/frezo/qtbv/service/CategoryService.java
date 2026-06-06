package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.CategoryFilter;
import com.frezo.qtbv.dto.request.CategoryRequest;
import com.frezo.qtbv.dto.response.CategoryResponse;
import com.frezo.qtbv.entity.Category;
import com.frezo.util.web.Response;

import java.util.Map;

public interface CategoryService  {
    Map<String, Object> all(CategoryFilter filter);
    Response<?> add(CategoryRequest request);
    Response<?> edit(String id, CategoryRequest request);
    CategoryResponse view (String id);
    void delete (String id);
}
