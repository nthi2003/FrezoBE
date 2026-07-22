package com.frezo.fbautomation.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class AffiliateLinkResponse {
    private String id;
    private String code;
    private String shortUrl;          // Full URL với domain, tiện copy-paste
    private String targetUrl;
    private String targetUrlWithUtm;  // targetUrl + UTM params (preview cho user)
    private String campaign;
    private String kolName;
    private String kolContact;
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmTerm;
    private String utmContent;
    private BigDecimal commissionRate;
    private String status;
    private OffsetDateTime expiresAt;
    private Long clickCount;
    private Long uniqueClickCount;
    private Long conversionCount;
    private BigDecimal revenue;
    private BigDecimal commissionPaid;
    private BigDecimal estimatedCommission; // revenue × commissionRate − commissionPaid
    private BigDecimal conversionRate;      // conversionCount / clickCount
    private String note;
    /** Kế thừa BaseEntity — LocalDateTime, không phải OffsetDateTime. */
    private LocalDateTime createdDate;
}
