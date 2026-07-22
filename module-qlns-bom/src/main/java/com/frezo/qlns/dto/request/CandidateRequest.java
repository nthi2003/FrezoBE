package com.frezo.qlns.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CandidateRequest {

    private String fullName;
    private String email;
    private String phone;
    private String source;
    private String currentPosition;
    private BigDecimal expectedSalary;
    private String cvUrl;
    private String linkedInUrl;
    private String notes;
}
