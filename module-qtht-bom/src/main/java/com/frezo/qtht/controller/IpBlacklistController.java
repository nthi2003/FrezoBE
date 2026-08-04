package com.frezo.qtht.controller;


import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
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
    @CheckPermission(api = "/qtht/ip-blacklist/ban", action = "CREATE")
    @Operation(summary = "Cấm truy cập một IP")
    public ApiResponse<IpBlacklist> banIp(@RequestBody Map<String, Object> body) {
        String ipAddress = (String) body.get("ipAddress");
        String reason = (String) body.get("reason");
        String bannedBy = (String) body.getOrDefault("bannedBy", "SYSTEM");
        Integer hours = body.containsKey("hours") ? Integer.valueOf(body.get("hours").toString()) : null;
        return ApiResponse.success(ipBlacklistService.addBan(ipAddress, reason, bannedBy, hours));
    }

    @DeleteMapping("/unban/{id}")
    @CheckPermission(api = "/qtht/ip-blacklist/unban/{id}", action = "DELETE")
    @Operation(summary = "Mở khóa một IP")
    public ApiResponse<String> unbanIp(@PathVariable String id) {
        ipBlacklistService.unban(id);
        return ApiResponse.success("Đã mở khóa IP");
    }

    @GetMapping
    @CheckPermission(api = "/qtht/ip-blacklist", action = "VIEW")
    @Operation(summary = "Lấy danh sách các IP đang bị cấm")
    public ApiResponse<List<IpBlacklist>> getActiveBans() {
        return ApiResponse.success(ipBlacklistService.getAllActiveBans());
    }
}
