package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.qtht.config.CheckPermission;
import com.frezo.qtht.dto.request.MenuPermissionSaveRequest;
import com.frezo.qtht.dto.response.MenuPermissionResponse;
import com.frezo.qtht.entity.Permission;
import com.frezo.qtht.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/qlht/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    @CheckPermission(api = "/qlht/permissions", action = "VIEW")
    public ApiResponse<List<MenuPermissionResponse>> getAll() {
        List<Permission> permissions = permissionRepository.findAll();
        List<MenuPermissionResponse> response = permissions.stream()
                .map(this::mapToResponse)
                .toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/combobox")
    public ApiResponse<List<ComboboxResponse>> getCombobox() {
        List<Permission> permissions = permissionRepository.findAll();

        if (permissions.isEmpty()) {
            return ApiResponse.success(Collections.emptyList());
        }

        List<ComboboxResponse> response = permissions.stream()
                .map(this::mapToCombobox)
                .toList();

        return ApiResponse.success(response);
    }

    @PostMapping
    @CheckPermission(api = "/qlht/permissions", action = "CREATE")
    public ApiResponse<MenuPermissionResponse> create(@RequestBody MenuPermissionSaveRequest request) {
        Permission permission = Permission.builder()
                .name(request.getName())
                .code(request.getCode())
                .apiPath(request.getApiPath())
                .apiMethod(request.getMethod())
                .action(request.getAction())
                .appCode(request.getAppCode())
                .build();

        Permission saved = permissionRepository.save(permission);
        return ApiResponse.success(mapToResponse(saved));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qlht/permissions", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable("id") String id) {
        permissionRepository.deleteById(id);
        return ApiResponse.success(null);
    }

    // --- Helper Methods ---

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

    private ComboboxResponse mapToCombobox(Permission p) {
        String name = StringUtils.hasText(p.getName()) ? p.getName() : "N/A";
        String action = StringUtils.hasText(p.getAction()) ? " (" + p.getAction() + ")" : "";
        String path = StringUtils.hasText(p.getApiPath()) ? p.getApiPath() : "";
        String method = StringUtils.hasText(p.getApiMethod()) ? " [" + p.getApiMethod() + "]" : "";

        return ComboboxResponse.builder()
                .value(p.getId()) // Use ID for combobox value
                .label(name + action)
                .description(path + method)
                .build();
    }
}
