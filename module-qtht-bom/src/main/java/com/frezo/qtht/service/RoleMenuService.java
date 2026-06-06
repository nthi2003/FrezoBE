package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.RoleMenuAssignRequest;
import com.frezo.qtht.dto.request.RolePermissionSaveRequest;
import com.frezo.qtht.dto.response.RoleMenuResponse;
import com.frezo.qtht.entity.RoleMenu;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RoleMenuService {
    RoleMenuResponse assignMenuToRole(RoleMenuAssignRequest request);

    List<RoleMenuResponse> getMenusByRole(String roleCode);

    void savePermissions(RolePermissionSaveRequest request);
}
