package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.qtht.config.CheckPermission;
import com.frezo.qtht.dto.request.OrganizationAddRequest;
import com.frezo.qtht.dto.request.OrganizationEditRequest;
import com.frezo.qtht.dto.request.OrganizationFilterRequest;
import com.frezo.qtht.dto.response.OrganizationResponse;
import com.frezo.qtht.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qtht/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @Operation(summary = "Lấy danh sách tổ chức", description = "Lấy danh sách tổ chức phân trang và tìm kiếm")
    @GetMapping
    @CheckPermission(api = "/qtht/organization", action = "VIEW")
    public ApiResponse<?> getAll(@ModelAttribute OrganizationFilterRequest filter) {
        return ApiResponse.success(organizationService.all(filter));
    }

    @Operation(summary = "Thêm mới tổ chức", description = "Thêm mới một tổ chức vào hệ thống")
    @PostMapping
    @CheckPermission(api = "/qtht/organization", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody OrganizationAddRequest request) {
        return ApiResponse.success(organizationService.add(request));
    }

    @Operation(summary = "Cập nhật tổ chức", description = "Cập nhật thông tin của một tổ chức")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qtht/organization", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody OrganizationEditRequest request) {
        return ApiResponse.success(organizationService.update(id, request));
    }

    @Operation(summary = "Xóa tổ chức", description = "Xóa mềm một tổ chức khỏi hệ thống")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qtht/organization", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        organizationService.delete(id);
        return ApiResponse.success("Xóa tổ chức thành công");
    }

    @Operation(summary = "Lấy danh sách combobox", description = "Lấy danh sách tổ chức dạng combobox")
    @GetMapping("/combobox")
    public ApiResponse<?> getCombobox(@ModelAttribute OrganizationFilterRequest filter) {
        Map<String, Object> data = organizationService.all(filter);
        Object itemsObj = data != null ? data.get("items") : null;

        if (!(itemsObj instanceof List<?> items)) {
            return ApiResponse.success(Collections.emptyList());
        }

        List<ComboboxResponse> comboboxData = items.stream()
                .filter(o -> o instanceof OrganizationResponse)
                .map(o -> (OrganizationResponse) o)
                .map(this::mapToCombobox)
                .toList();

        return ApiResponse.success(comboboxData);
    }

    private ComboboxResponse mapToCombobox(OrganizationResponse org) {
        return ComboboxResponse.builder()
                .value(org.getId())
                .label(org.getName())
                .description(org.getCode())
                .build();
    }
}
