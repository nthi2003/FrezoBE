package com.frezo.dmdc.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.dmdc.dto.response.UxPopupResponse;
import com.frezo.dmdc.service.UxPopupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Resolve UX success popup templates (seeded as category group {@code UX_POPUP}).
 * Admin CRUD nội dung qua {@code /qtht/category} (groupCode=UX_POPUP).
 */
@RestController
@RequestMapping("/qtht/ux-popups")
@RequiredArgsConstructor
@Tag(name = "UX Popups", description = "Template popup thành công theo event code")
public class UxPopupController {

    private final UxPopupService uxPopupService;

    @GetMapping("/{eventCode}")
    @CheckPermission(api = "/qtht/ux-popups/{eventCode}", action = "VIEW")
    @Operation(summary = "Lấy template popup theo event code (user đã đăng nhập)")
    public ApiResponse<UxPopupResponse> getByEventCode(@PathVariable String eventCode) {
        return ApiResponse.ok(uxPopupService.resolve(eventCode).orElse(null));
    }
}
