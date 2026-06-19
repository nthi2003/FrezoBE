package com.frezo.qlns.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PayrollPeriodResponse {
    private String id;
    private String orgId;
    private Integer month;
    private Integer year;
    private String name;
    private Integer status;
    private String statusLabel;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate paymentDate;
    private LocalDateTime lockedAt;
    private String lockedBy;
    private String note;
}
