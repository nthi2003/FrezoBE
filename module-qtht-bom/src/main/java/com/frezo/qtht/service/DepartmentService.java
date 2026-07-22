package com.frezo.qtht.service;

import com.frezo.common.response.PageResponse;
import com.frezo.qtht.dto.request.DepartmentFilterRequest;
import com.frezo.qtht.dto.request.DepartmentSaveRequest;
import com.frezo.qtht.dto.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    /**
     * v1.1 (Batch F): return type-safe {@link PageResponse} thay cho {@code Map<String,Object>}.
     */
    PageResponse<DepartmentResponse> all(DepartmentFilterRequest filter);

    List<DepartmentResponse> getTree();

    DepartmentResponse create(DepartmentSaveRequest request);

    DepartmentResponse update(String id, DepartmentSaveRequest request);

    void delete(String id);

    void activate(String id);

    void deactivate(String id);
}
