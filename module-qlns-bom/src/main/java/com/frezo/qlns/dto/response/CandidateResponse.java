package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponse {

    private String id;
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
