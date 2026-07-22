package com.frezo.qtht.controller;


import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.entity.IpBlacklist;
import com.frezo.qtht.service.IpBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qtht/ip-blacklist")
@RequiredArgsConstructor
@Tag(name = "IpBlacklistController", description = "Quản lý danh sách IP bị cấm")
public class IpBlacklistController {

    private final IpBlacklistService ipBlacklistService;

    @PostMapping("/ban")
    @Operation(summary = "Cấm truy cập một IP")
    public ApiResponse<IpBlacklist> banIp(@RequestBody Map<String, Object> body) {
        String ipAddress = (String) body.get("ipAddress");
        String reason = (String) body.get("reason");
        String bannedBy = (String) body.getOrDefault("bannedBy", "SYSTEM");
        Integer hours = body.containsKey("hours") ? Integer.valueOf(body.get("hours").toString()) : null;
        return ApiResponse.success(ipBlacklistService.addBan(ipAddress, reason, bannedBy, hours));
    }

    @DeleteMapping("/unban/{id}")
    @Operation(summary = "Mở khóa một IP")
    public ApiResponse<String> unbanIp(@PathVariable String id) {
        ipBlacklistService.unban(id);
        return ApiResponse.success("Đã mở khóa IP");
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách các IP đang bị cấm")
    public ApiResponse<List<IpBlacklist>> getActiveBans() {
        return ApiResponse.success(ipBlacklistService.getAllActiveBans());
    }
}
