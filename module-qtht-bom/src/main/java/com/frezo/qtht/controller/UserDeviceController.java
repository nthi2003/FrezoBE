package com.frezo.qtht.controller;

import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.dto.request.DeviceRegisterRequest;
import com.frezo.qtht.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/qtht/user-device")
@RequiredArgsConstructor
@Tag(name = "User Device / Push", description = "Đăng ký thiết bị nhận push notification")
public class UserDeviceController {

    private final PushNotificationService pushService;

    @Operation(summary = "Đăng ký / cập nhật Expo push token của device hiện tại")
    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody DeviceRegisterRequest req) {
        String me = SystemUtils.getCurrentUsername();
        pushService.registerDevice(me, req.getExpoPushToken(),
                req.getPlatform(), req.getDeviceName(),
                req.getDeviceId(), req.getAppVersion());
        return ApiResponse.success(Map.of("status", "OK"));
    }

    @Operation(summary = "Huỷ đăng ký device (khi logout)")
    @PostMapping("/unregister")
    public ApiResponse<?> unregister(@RequestBody Map<String, String> body) {
        String me = SystemUtils.getCurrentUsername();
        String token = body != null ? body.get("expoPushToken") : null;
        pushService.unregisterDevice(me, token);
        return ApiResponse.success(Map.of("status", "OK"));
    }
}
