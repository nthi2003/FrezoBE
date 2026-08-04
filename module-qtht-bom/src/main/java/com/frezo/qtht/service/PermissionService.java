package com.frezo.qtht.service;

import com.frezo.common.response.ComboboxResponse;
import com.frezo.qtht.dto.request.MenuPermissionSaveRequest;
import com.frezo.qtht.dto.response.MenuPermissionResponse;

import java.util.List;

public interface PermissionService {

    List<MenuPermissionResponse> findAll();

    List<ComboboxResponse> getCombobox();

    MenuPermissionResponse create(MenuPermissionSaveRequest request);

    void delete(String id);
}
