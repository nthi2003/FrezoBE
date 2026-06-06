package com.frezo.qtbv.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtbv.service.ManagerService;
import com.frezo.qtbv.service.OrganizationCommonService;
import com.frezo.qtht.config.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qtbv/articles")
@RequiredArgsConstructor
@Tag(name = "7. Danh sách dùng chung", description = "API lấy danh sách manager và tổ chức cho module quản lý bài viết")
public class CommonController {

    private final ManagerService managerService;
    private final OrganizationCommonService organizationCommonService;

    @Operation(summary = "Lấy danh sách manager", description = "Lấy tất cả manager đang hoạt động để dùng cho select")
    @GetMapping("/managers")
    @CheckPermission(api = "/qtbv/managers", action = "VIEW")
    public ApiResponse<?> getManagers() {
        return ApiResponse.success(managerService.getManagersList());
    }

    @Operation(summary = "Lấy danh sách tổ chức", description = "Lấy tất cả tổ chức trong hệ thống để dùng cho select")
    @GetMapping("/organizations")
    @CheckPermission(api = "/qtbv/organizations", action = "VIEW")
    public ApiResponse<?> getOrganizations() {
        return ApiResponse.success(organizationCommonService.getOrganizationsList());
    }
}
