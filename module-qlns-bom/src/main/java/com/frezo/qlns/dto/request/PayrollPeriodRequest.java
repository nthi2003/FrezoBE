package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PayrollPeriodRequest {
    private String orgId;
    private Integer month;
    private Integer year;
    private String name;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate paymentDate;
    private String note;
}
