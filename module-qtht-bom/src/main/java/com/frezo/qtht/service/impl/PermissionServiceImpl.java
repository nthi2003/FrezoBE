package com.frezo.qtht.service.impl;

import com.frezo.common.response.ComboboxResponse;
import com.frezo.qtht.dto.request.MenuPermissionSaveRequest;
import com.frezo.qtht.dto.response.MenuPermissionResponse;
import com.frezo.qtht.entity.Permission;
import com.frezo.qtht.repository.PermissionRepository;
import com.frezo.qtht.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    /**
     * Lấy tất cả quyền (permission) dưới dạng danh sách phản hồi chi tiết.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MenuPermissionResponse> findAll() {
        return permissionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Lấy danh sách quyền dưới dạng combobox để hiển thị trên giao diện chọn.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ComboboxResponse> getCombobox() {
        List<Permission> permissions = permissionRepository.findAll();
        if (permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream().map(this::mapToCombobox).toList();
    }

    /**
     * Tạo mới một quyền (permission) từ dữ liệu yêu cầu.
     */
    @Override
    @Transactional
    public MenuPermissionResponse create(MenuPermissionSaveRequest request) {
        Permission permission = Permission.builder()
                .name(request.getName())
                .code(request.getCode())
                .apiPath(request.getApiPath())
                .apiMethod(request.getMethod())
                .action(request.getAction())
                .appCode(request.getAppCode())
                .build();
        return mapToResponse(permissionRepository.save(permission));
    }

    /**
     * Xóa vĩnh viễn một quyền theo mã định danh.
     */
    @Override
    @Transactional
    public void delete(String id) {
        permissionRepository.deleteById(id);
    }

    /**
     * Chuyển đổi entity Permission sang MenuPermissionResponse.
     */
    private MenuPermissionResponse mapToResponse(Permission p) {
        return MenuPermissionResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .code(p.getCode())
                .apiPath(p.getApiPath())
                .method(p.getApiMethod())
                .action(p.getAction())
                .build();
    }

    /**
     * Chuyển đổi entity Permission sang ComboboxResponse (label = tên + action, description = đường dẫn + method).
     */
    private ComboboxResponse mapToCombobox(Permission p) {
        String name = StringUtils.hasText(p.getName()) ? p.getName() : "N/A";
        String action = StringUtils.hasText(p.getAction()) ? " (" + p.getAction() + ")" : "";
        String path = StringUtils.hasText(p.getApiPath()) ? p.getApiPath() : "";
        String method = StringUtils.hasText(p.getApiMethod()) ? " [" + p.getApiMethod() + "]" : "";

        return ComboboxResponse.builder()
                .value(p.getId())
                .label(name + action)
                .description(path + method)
                .build();
    }
}
