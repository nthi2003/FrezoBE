package com.frezo.qtht.controller;

import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.config.CheckPermission;
import com.frezo.qtht.entity.IpWhitelist;
import com.frezo.qtht.service.IpWhitelistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ip-whitelist")
@RequiredArgsConstructor
@Tag(name = "IP Whitelist", description = "Quản lý danh sách IP được phép truy cập")
public class IpWhitelistController {

    private final IpWhitelistService ipWhitelistService;

    @Operation(summary = "Lấy danh sách IP whitelist", description = "Lấy tất cả các IP đang được cho phép")
    @GetMapping
    @CheckPermission(api = "/ip-whitelist", action = "VIEW")
    public ResponseEntity<ApiResponse<List<IpWhitelist>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.success(ipWhitelistService.getAllActive()));
    }

    @Operation(summary = "Thêm IP vào whitelist", description = "Thêm một địa chỉ IP vào danh sách cho phép")
    @PostMapping
    @CheckPermission(api = "/ip-whitelist", action = "CREATE")
    public ResponseEntity<ApiResponse<IpWhitelist>> addToWhitelist(
            @RequestParam String ipAddress,
            @RequestParam(required = false) String description) {
        String createdBy = SystemUtils.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(ipWhitelistService.addToWhitelist(ipAddress, description, createdBy)));
    }

    @Operation(summary = "Xóa IP khỏi whitelist", description = "Vô hiệu hóa một IP trong danh sách whitelist")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/ip-whitelist", action = "DELETE")
    public ResponseEntity<ApiResponse<String>> removeFromWhitelist(@PathVariable String id) {
        ipWhitelistService.removeFromWhitelist(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa IP khỏi whitelist"));
    }

    @Operation(summary = "Kiểm tra IP có trong whitelist", description = "Kiểm tra xem một IP có được cho phép không")
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> isWhitelisted(@RequestParam String ipAddress) {
        return ResponseEntity.ok(ApiResponse.success(ipWhitelistService.isWhitelisted(ipAddress)));
    }
}

