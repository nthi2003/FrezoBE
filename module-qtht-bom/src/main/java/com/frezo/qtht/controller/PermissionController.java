package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qtht.dto.request.MenuPermissionSaveRequest;
import com.frezo.qtht.dto.response.MenuPermissionResponse;
import com.frezo.qtht.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qtht/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @CheckPermission(api = "/qtht/permission", action = "VIEW")
    public ApiResponse<List<MenuPermissionResponse>> getAll() {
        return ApiResponse.success(permissionService.findAll());
    }

    @GetMapping("/combobox")
    public ApiResponse<List<ComboboxResponse>> getCombobox() {
        return ApiResponse.success(permissionService.getCombobox());
    }

    @PostMapping
    @CheckPermission(api = "/qtht/permission", action = "CREATE")
    public ApiResponse<MenuPermissionResponse> create(@RequestBody MenuPermissionSaveRequest request) {
        return ApiResponse.success(permissionService.create(request));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qtht/permission/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable("id") String id) {
        permissionService.delete(id);
        return ApiResponse.success(null);
    }
}
