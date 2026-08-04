package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AdCampaignRequest {
    @NotBlank(message = "Tên chiến dịch bắt buộc")
    @Size(max = 255)
    private String name;

    @Size(max = 32) private String platform;
    @Size(max = 32) private String objective;
    @Size(max = 16) private String status;

    private BigDecimal budget;
    private BigDecimal spend;
    private Long impressions;
    private Long clicks;
    private Long leads;
    private BigDecimal revenue;
    private LocalDate startDate;
    private LocalDate endDate;

    @Size(max = 128) private String externalAdId;
    @Size(max = 1000) private String landingUrl;
    private String note;
}
