package com.frezo.qtht.dto.request;

import lombok.Data;

@Data
public class DeviceRegisterRequest {
    private String expoPushToken;
    private String platform;    // ios | android | web
    private String deviceName;
    private String deviceId;
    private String appVersion;
}
