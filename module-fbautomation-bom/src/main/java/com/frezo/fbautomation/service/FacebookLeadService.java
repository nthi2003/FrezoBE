package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.response.FacebookLeadResponse;

import java.util.List;

public interface FacebookLeadService {

    /**
     * Danh sách lead — filter theo status + source cùng lúc.
     * Cả 2 param đều optional (null | blank | "all" = bỏ qua).
     */
    List<FacebookLeadResponse> getAll(String status, String source);

    /** Legacy 1-param (mặc định source=null). Giữ để backward compat. */
    default List<FacebookLeadResponse> getAll(String status) {
        return getAll(status, null);
    }

    FacebookLeadResponse getById(String id);

    void delete(String id);

    String importToCustomer(String id);

    int importAllToCustomer(List<String> ids);

    long countByStatus(String status);

    /** Assign lead cho 1 nhân viên CSKH (username). */
    FacebookLeadResponse assign(String id, String username);
}
