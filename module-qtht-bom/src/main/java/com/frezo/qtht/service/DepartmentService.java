package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.DepartmentFilterRequest;
import com.frezo.qtht.dto.request.DepartmentSaveRequest;
import com.frezo.qtht.dto.response.DepartmentResponse;
import java.util.List;
import java.util.Map;

public interface DepartmentService {
    Map<String, Object> all(DepartmentFilterRequest filter);

    List<DepartmentResponse> getTree();

    DepartmentResponse create(DepartmentSaveRequest request);

    DepartmentResponse update(String id, DepartmentSaveRequest request);

    void delete(String id);

    void activate(String id);

    void deactivate(String id);
}
