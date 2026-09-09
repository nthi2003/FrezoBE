package com.frezo.auth.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CreateUserSession {

    private final String username;
    private final String ipAddress;
    private final String userAgent;
    private final String deviceInfo;
    private final LocalDateTime now;
}
