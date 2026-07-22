package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OnboardingAssignRequest {
    private String templateId;
    private String personId;
    private LocalDate startDate;
}
