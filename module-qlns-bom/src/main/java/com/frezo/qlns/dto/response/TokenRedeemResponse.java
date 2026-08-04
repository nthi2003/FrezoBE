package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TokenRedeemResponse {
    private String id;
    private String personId;
    private String personName;
    private BigDecimal amount;
    private BigDecimal cashValue;
    private String note;
    private String status;
    private String payrollPeriodId;
    private Integer targetMonth;
    private Integer targetYear;
    private String reviewedBy;
    private String reviewedAt;
    private String rejectReason;
    private String createdDate;
}
