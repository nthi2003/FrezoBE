package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.entity.IpBlacklist;
import com.frezo.qtht.entity.IpWhitelist;
import com.frezo.qtht.service.IpBlacklistService;
import com.frezo.qtht.service.IpWhitelistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/qtht/internal-gateway")
@RequiredArgsConstructor
@Tag(name = "Internal Gateway API", description = "Các API nội bộ phục vụ cho API Gateway")
public class InternalGatewayController {

    private final IpBlacklistService ipBlacklistService;
    private final IpWhitelistService ipWhitelistService;

    @GetMapping("/blacklist")
    @Operation(summary = "Lấy danh sách IP bị cấm cho Gateway")
    public ApiResponse<List<String>> getBlacklist() {
        List<String> ips = ipBlacklistService.getAllActiveBans().stream()
                .map(IpBlacklist::getIpAddress)
                .collect(Collectors.toList());
        return ApiResponse.success(ips);
    }

    @GetMapping("/whitelist")
    @Operation(summary = "Lấy danh sách IP được phép cho Gateway")
    public ApiResponse<List<String>> getWhitelist() {
        List<String> ips = ipWhitelistService.getAllActive().stream()
                .map(IpWhitelist::getIpAddress)
                .collect(Collectors.toList());
        return ApiResponse.success(ips);
    }

    @PostMapping("/block-ip")
    @Operation(summary = "Chặn IP do vi phạm rate limit")
    public ApiResponse<IpBlacklist> blockIp(@RequestParam String ipAddress,
                                            @RequestParam String reason,
                                            @RequestParam int blockDurationMinutes) {
        IpBlacklist ban = ipBlacklistService.addBanMinutes(ipAddress, reason, "GATEWAY", blockDurationMinutes);
        return ApiResponse.success(ban);
    }
}
