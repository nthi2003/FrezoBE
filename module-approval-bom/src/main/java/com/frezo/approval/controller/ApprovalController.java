package com.frezo.approval.controller;

import com.frezo.approval.dto.ApprovalActionPayload;
import com.frezo.approval.dto.ApprovalRequestDto;
import com.frezo.approval.dto.ApprovalStepDto;
import com.frezo.approval.dto.CreateApprovalRequest;
import com.frezo.approval.service.ApprovalService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.FePage;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
@Tag(name = "Approval — Inbox", description = "Hộp thư duyệt + timeline")
public class ApprovalController {

    private final ApprovalService approvalService;

    @Operation(summary = "Inbox của tôi")
    @GetMapping("/my")
    @CheckPermission(api = "/approvals/my", action = "VIEW")
    public ApiResponse<FePage<ApprovalRequestDto>> listMy(
            @RequestParam(defaultValue = "pending") String status) {
        return ApiResponse.ok(approvalService.listMy(status));
    }

    @Operation(summary = "Duyệt")
    @PostMapping("/{id}/approve")
    @CheckPermission(api = "/approvals/{id}/approve", action = "APPROVE")
    public ApiResponse<ApprovalRequestDto> approve(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalActionPayload payload) {
        String comment = payload != null ? payload.getComment() : null;
        return ApiResponse.ok(approvalService.approve(id, comment));
    }

    @Operation(summary = "Từ chối")
    @PostMapping("/{id}/reject")
    @CheckPermission(api = "/approvals/{id}/reject", action = "APPROVE")
    public ApiResponse<ApprovalRequestDto> reject(
            @PathVariable String id,
            @RequestBody(required = false) ApprovalActionPayload payload) {
        String comment = payload != null ? payload.getComment() : null;
        return ApiResponse.ok(approvalService.reject(id, comment));
    }

    @Operation(summary = "Timeline theo request id")
    @GetMapping("/{id}/timeline")
    @CheckPermission(api = "/approvals/{id}/timeline", action = "VIEW")
    public ApiResponse<List<ApprovalStepDto>> timeline(@PathVariable String id) {
        return ApiResponse.ok(approvalService.timeline(id));
    }

    @Operation(summary = "Timeline theo subject (embed LeavesPage)")
    @GetMapping("/timeline")
    @CheckPermission(api = "/approvals/timeline", action = "VIEW")
    public ApiResponse<List<ApprovalStepDto>> timelineBySubject(
            @RequestParam String subjectType,
            @RequestParam String subjectId) {
        return ApiResponse.ok(approvalService.timelineBySubject(subjectType, subjectId));
    }

    @Operation(summary = "Approval request theo subject")
    @GetMapping("/by-subject")
    @CheckPermission(api = "/approvals/by-subject", action = "VIEW")
    public ApiResponse<ApprovalRequestDto> bySubject(
            @RequestParam String subjectType,
            @RequestParam String subjectId) {
        return ApiResponse.ok(approvalService.findBySubject(subjectType, subjectId));
    }

    @Operation(summary = "Tạo request (internal — module khác gọi)")
    @PostMapping
    @CheckPermission(api = "/approvals", action = "CREATE")
    public ApiResponse<ApprovalRequestDto> create(@RequestBody CreateApprovalRequest req) {
        return ApiResponse.ok(approvalService.create(req));
    }
}
