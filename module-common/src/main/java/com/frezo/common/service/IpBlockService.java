package com.frezo.common.service;

import com.frezo.common.constant.BlockReason;

public interface IpBlockService {
    void checkIpBlocked(String ipAddress, String userName);

    void handleFailedAttempt(String ipAddress, String targetUserName, BlockReason reason);

    void clearFailedAttempts(String ipAddress, String userName);
}
