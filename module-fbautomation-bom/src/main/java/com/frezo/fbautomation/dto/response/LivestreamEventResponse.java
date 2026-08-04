package com.frezo.fbautomation.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class LivestreamEventResponse {
    private String id;
    private String title;
    private String channel;
    private OffsetDateTime scheduledAt;
    private Integer durationMinutes;
    private Integer notifyBeforeMinutes;
    private String status;
    private Integer registrantCount;
    private OffsetDateTime notifiedAt;
    private String streamUrl;
    private String note;
    private Boolean needsNotify;
    private LocalDateTime createdDate;
}
