package com.frezo.qtht.controller;


import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.entity.IpBlacklist;
import com.frezo.qtht.service.IpBlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/ip-blacklist")
@RequiredArgsConstructor
@Tag(name = "IpBlacklistController", description = "Quản lý danh sách IP bị cấm")
public class IpBlacklistController {

    private final IpBlacklistService ipBlacklistService;

    @PostMapping("/ban")
    @Operation(summary = "Cấm truy cập một IP")
    public ApiResponse<IpBlacklist> banIp(@RequestParam String ip,
                                        @RequestParam String reason,
                                        @RequestParam String bannedBy,
                                        @RequestParam(required = false) Integer hours) {
        return ApiResponse.success(ipBlacklistService.addBan(ip, reason, bannedBy, hours));
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
