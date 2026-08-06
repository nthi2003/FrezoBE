package com.frezo.common.service;

import com.frezo.common.constant.BlockReason;

public interface IpBlockService {
    void checkIpBlocked(String ipAddress, String userName);

    void handleFailedAttempt(String ipAddress, String targetUserName, BlockReason reason);

    void clearFailedAttempts(String ipAddress, String userName);

    /**
     * Khóa tài khoản và đưa IP vào blacklist (bảng {@code ip_blacklist}) để admin theo dõi
     * ở trang Bảo mật hệ thống. Dùng khi phát hiện dò mã (VD sai OTP quá số lần cho phép).
     *
     * @param banMinutes thời hạn ban; {@code null} = ban vô thời hạn tới khi admin bỏ chặn
     */
    void lockUserAndBlacklistIp(String ipAddress, String userName, BlockReason reason, Integer banMinutes);
}
