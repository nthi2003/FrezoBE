package com.frezo.approval.controller;

import com.frezo.approval.dto.ApprovalFlowDto;
import com.frezo.approval.dto.ApprovalFlowRequest;
import com.frezo.approval.service.ApprovalFlowService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/approval-flows")
@RequiredArgsConstructor
@Tag(name = "Approval — Flow config", description = "Cấu hình luồng duyệt")
public class ApprovalFlowController {

    private final ApprovalFlowService approvalFlowService;

    @GetMapping
    @CheckPermission(api = "/approval-flows", action = "VIEW")
    @Operation(summary = "Danh sách flow")
    public ApiResponse<List<ApprovalFlowDto>> list() {
        return ApiResponse.ok(approvalFlowService.list());
    }

    @PostMapping
    @CheckPermission(api = "/approval-flows", action = "CREATE")
    @Operation(summary = "Tạo flow")
    public ApiResponse<ApprovalFlowDto> create(@RequestBody ApprovalFlowRequest req) {
        return ApiResponse.ok(approvalFlowService.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/approval-flows/{id}", action = "UPDATE")
    @Operation(summary = "Cập nhật flow")
    public ApiResponse<ApprovalFlowDto> update(@PathVariable String id,
                                               @RequestBody ApprovalFlowRequest req) {
        return ApiResponse.ok(approvalFlowService.update(id, req));
    }
}
