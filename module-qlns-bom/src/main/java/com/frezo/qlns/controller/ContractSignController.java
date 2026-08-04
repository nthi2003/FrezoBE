package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.service.ContractSignService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/qlns/contracts")
@RequiredArgsConstructor
@Tag(name = "QLNS — Contract OTP e-sign")
public class ContractSignController {

    private final ContractSignService contractSignService;

    @GetMapping("/{id}/sign/status")
    @CheckPermission(api = "/qlns/contracts/{id}/sign/status", action = "VIEW")
    public ApiResponse<Map<String, Object>> status(@PathVariable String id) {
        return ApiResponse.ok(contractSignService.status(id));
    }

    @PostMapping("/{id}/sign/request-otp")
    @CheckPermission(api = "/qlns/contracts/{id}/sign/request-otp", action = "UPDATE")
    public ApiResponse<Map<String, Object>> requestOtp(
            @PathVariable String id, HttpServletRequest http) {
        return ApiResponse.ok(contractSignService.requestOtp(
                id, clientIp(http), http.getHeader("User-Agent")));
    }

    @PostMapping("/{id}/sign/confirm")
    @CheckPermission(api = "/qlns/contracts/{id}/sign/confirm", action = "UPDATE")
    public ApiResponse<Map<String, Object>> confirm(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            HttpServletRequest http) {
        return ApiResponse.ok(contractSignService.confirm(
                id, body.get("otp"), clientIp(http), http.getHeader("User-Agent")));
    }

    private static String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
