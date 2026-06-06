package com.frezo.qlns.dto.request;

import lombok.Data;

@Data
public class PayrollFilter {
    private String contractId;
    private String personId;
    private Integer month;
    private Integer year;
    private Integer status; // 0=DRAFT, 1=CONFIRMED, 2=PAID
    private Integer pageNumber;
    private Integer pageSize = 10;
}
