package com.frezo.crm.dto;

import com.frezo.crm.common.ActivityType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityRequest {
    private String dealId;
    private String customerId;
    @NotNull
    private ActivityType activityType;
    private String subject;
    private String content;
    private LocalDateTime happenedAt;
    private String ownerUsername;
}
