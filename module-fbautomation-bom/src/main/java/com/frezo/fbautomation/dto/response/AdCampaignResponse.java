package com.frezo.fbautomation.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AdCampaignResponse {
    private String id;
    private String name;
    private String platform;
    private String objective;
    private String status;
    private BigDecimal budget;
    private BigDecimal spend;
    private Long impressions;
    private Long clicks;
    private Long leads;
    private BigDecimal revenue;
    private LocalDate startDate;
    private LocalDate endDate;
    private String externalAdId;
    private String landingUrl;
    private String note;
    private LocalDateTime createdDate;
    /** clicks / impressions */
    private BigDecimal ctr;
    /** spend / clicks */
    private BigDecimal cpc;
    /** spend / leads */
    private BigDecimal cpl;
    /** revenue / spend */
    private BigDecimal roas;
}
