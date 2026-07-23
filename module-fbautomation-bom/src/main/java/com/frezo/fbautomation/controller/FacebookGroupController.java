package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.response.FacebookGroupResponse;
import com.frezo.fbautomation.service.FacebookGroupService;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fb/groups")
@RequiredArgsConstructor
@Tag(name = "Facebook Groups", description = "Danh sách Group Facebook đã quét")
public class FacebookGroupController {

    private final FacebookGroupService groupService;

    @Operation(summary = "Danh sách groups (lọc theo status)")
    @GetMapping
    public ApiResponse<List<FacebookGroupResponse>> getAll(
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(groupService.getAll(status));
    }

    @Operation(summary = "Chi tiết group")
    @GetMapping("/{id}")
    public ApiResponse<FacebookGroupResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(groupService.getById(id));
    }

    @Operation(summary = "Xóa group")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        groupService.delete(id);
        return ApiResponse.ok();
    }
}
