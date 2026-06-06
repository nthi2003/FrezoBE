package com.frezo.qtht.service;


import com.frezo.qtht.dto.request.RoleCreateRequest;
import com.frezo.qtht.dto.request.RoleUpdateRequest;
import com.frezo.qtht.dto.response.RoleResponse;
import com.frezo.qtht.entity.Role;

import java.util.List;

public interface RoleService {

    RoleResponse create(RoleCreateRequest request);

    RoleResponse update(RoleUpdateRequest request);

    List<RoleResponse> getRoleByAppCode(String appCode);

    void delete(String code, String appCode);


    List<RoleResponse> getAll();
}

