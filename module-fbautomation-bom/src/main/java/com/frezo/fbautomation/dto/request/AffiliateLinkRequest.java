package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class AffiliateLinkRequest {

    /** Nếu bỏ trống → BE tự sinh slug 8 ký tự. */
    @Size(max = 32) private String code;

    @NotBlank(message = "Vui lòng nhập URL đích")
    @Size(max = 2000)
    private String targetUrl;

    @Size(max = 255) private String campaign;
    @Size(max = 255) private String kolName;
    @Size(max = 255) private String kolContact;

    @Size(max = 100) private String utmSource;
    @Size(max = 100) private String utmMedium;
    @Size(max = 100) private String utmCampaign;
    @Size(max = 100) private String utmTerm;
    @Size(max = 100) private String utmContent;

    private BigDecimal commissionRate;
    private OffsetDateTime expiresAt;
    private String note;
}
