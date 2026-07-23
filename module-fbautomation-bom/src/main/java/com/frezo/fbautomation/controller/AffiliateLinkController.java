package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.request.AffiliateLinkRequest;
import com.frezo.fbautomation.dto.response.AffiliateLinkResponse;
import com.frezo.fbautomation.service.AffiliateLinkService;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * AffiliateLinkController — CRUD affiliate link + dashboard KOL.
 * <p>
 * Endpoint public redirect nằm ở {@link PublicRedirectController} — không cần auth.
 */
@RestController
@RequestMapping("/mkt/affiliate")
@RequiredArgsConstructor
@Tag(name = "MKT · Affiliate/KOL", description = "Sinh short link + tracker conversion cho KOL/CTV")
public class AffiliateLinkController {

    private final AffiliateLinkService service;

    @GetMapping
    public ApiResponse<List<AffiliateLinkResponse>> list(
            @RequestParam(required = false) String campaign,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kolName) {
        return ApiResponse.ok(service.list(campaign, status, kolName));
    }

    @GetMapping("/{id}")
    public ApiResponse<AffiliateLinkResponse> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @Operation(summary = "Tạo affiliate link mới")
    public ApiResponse<AffiliateLinkResponse> create(@RequestBody @Valid AffiliateLinkRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<AffiliateLinkResponse> update(
            @PathVariable String id,
            @RequestBody @Valid AffiliateLinkRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard tổng hợp affiliate")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }

    @PostMapping("/{code}/convert")
    @Operation(summary = "Ghi nhận conversion (gọi từ order/lead flow)")
    public ApiResponse<Boolean> recordConversion(
            @PathVariable String code,
            @RequestParam(required = false) BigDecimal value) {
        return ApiResponse.ok(service.recordConversion(code, value));
    }
}
