package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.request.JoinGroupRequest;
import com.frezo.fbautomation.dto.request.ScanGroupRequest;
import com.frezo.fbautomation.dto.response.AutomationSummaryResponse;
import com.frezo.fbautomation.dto.response.FacebookGroupResponse;
import com.frezo.fbautomation.service.FacebookAutomationService;
import com.frezo.util.web.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/fb/automation")
@RequiredArgsConstructor
@Tag(name = "Facebook Automation", description = "Tác vụ tự động hóa Facebook (Selenium)")
public class FacebookAutomationController {

    private final FacebookAutomationService automationService;

    @Operation(summary = "Quét và scrape groups từ keyword")
    @PostMapping("/scan-groups")
    public Response<String> scanGroups(@RequestBody ScanGroupRequest request) {
        CompletableFuture<List<FacebookGroupResponse>> future =
                automationService.searchAndScrapeGroups(
                        request.getAccountId(),
                        request.getKeyword(),
                        request.getMaxResults());
        return Response.ok("Yêu cầu quét groups đã được gửi. Vui lòng kiểm tra kết quả sau vài phút.");
    }

    @Operation(summary = "Tự động tham gia group")
    @PostMapping("/join-group")
    public Response<String> joinGroup(@RequestBody JoinGroupRequest request) {
        CompletableFuture<String> future =
                automationService.autoJoinGroup(request.getAccountId(), request.getGroupId());
        return Response.ok("Yêu cầu tham gia group đã được gửi.");
    }

    @Operation(summary = "Đăng nhập tài khoản để refresh cookie")
    @PostMapping("/login/{accountId}")
    public Response<String> login(@PathVariable String accountId) {
        automationService.loginWithCookie(accountId);
        return Response.ok("Đã đăng nhập thành công");
    }

    @Operation(summary = "Tổng quan hệ thống")
    @GetMapping("/summary")
    public Response<AutomationSummaryResponse> summary() {
        return Response.ok(automationService.getSummary());
    }
}
