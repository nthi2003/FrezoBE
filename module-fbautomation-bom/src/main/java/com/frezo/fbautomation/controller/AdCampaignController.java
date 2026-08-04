package com.frezo.fbautomation.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.fbautomation.dto.request.AdCampaignRequest;
import com.frezo.fbautomation.dto.response.AdCampaignResponse;
import com.frezo.fbautomation.service.AdCampaignService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mkt/ads")
@RequiredArgsConstructor
@Tag(name = "MKT · Ads", description = "Theo dõi chiến dịch Ads (MVP thủ công — chưa Meta Marketing API)")
public class AdCampaignController {

    private final AdCampaignService service;

    @GetMapping("/dashboard")
    @CheckPermission(api = "/mkt/ads/dashboard", action = "VIEW")
    @Operation(summary = "Dashboard tổng hợp Ads")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }

    @GetMapping
    @CheckPermission(api = "/mkt/ads", action = "VIEW")
    public ApiResponse<List<AdCampaignResponse>> list(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(platform, status));
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/mkt/ads/{id}", action = "VIEW")
    public ApiResponse<AdCampaignResponse> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @CheckPermission(api = "/mkt/ads", action = "CREATE")
    public ApiResponse<AdCampaignResponse> create(@RequestBody @Valid AdCampaignRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/mkt/ads/{id}", action = "UPDATE")
    public ApiResponse<AdCampaignResponse> update(
            @PathVariable String id,
            @RequestBody @Valid AdCampaignRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/mkt/ads/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok();
    }
}
