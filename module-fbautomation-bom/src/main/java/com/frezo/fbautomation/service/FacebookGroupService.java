package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.response.FacebookGroupResponse;
import com.frezo.fbautomation.entity.FacebookGroup;

import java.util.List;

public interface FacebookGroupService {

    List<FacebookGroupResponse> getAll(String status);

    FacebookGroupResponse getById(String id);

    void delete(String id);

    long countByStatus(String status);
}
