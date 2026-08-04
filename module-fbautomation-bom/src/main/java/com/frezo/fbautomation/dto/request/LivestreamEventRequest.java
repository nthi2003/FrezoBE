package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class LivestreamEventRequest {
    @NotBlank(message = "Tiêu đề bắt buộc")
    @Size(max = 500)
    private String title;

    @Size(max = 32) private String channel;

    @NotNull(message = "Thời gian live bắt buộc")
    private OffsetDateTime scheduledAt;

    private Integer durationMinutes;
    private Integer notifyBeforeMinutes;
    @Size(max = 16) private String status;
    private Integer registrantCount;
    @Size(max = 1000) private String streamUrl;
    private String note;
}
