package com.frezo.qtht.service;

import java.util.List;
import java.util.Map;

/**
 * Gửi push notification qua Expo Push API (https://exp.host/--/api/v2/push/send).
 * Fire-and-forget: fail 1 message không block phần còn lại.
 * Auto de-active token nếu Expo trả DeviceNotRegistered.
 */
public interface PushNotificationService {

    /** Đăng ký / cập nhật device của user hiện tại. */
    void registerDevice(String username, String expoPushToken,
                        String platform, String deviceName,
                        String deviceId, String appVersion);

    /** Unregister — dùng khi logout. */
    void unregisterDevice(String username, String expoPushToken);

    /** Push cho 1 user (mọi device active). */
    void sendToUser(String username, String title, String body, Map<String, Object> data);

    /** Push cho nhiều user (mỗi user có thể có nhiều device). */
    void sendToUsers(List<String> usernames, String title, String body, Map<String, Object> data);
}
