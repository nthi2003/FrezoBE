package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingAssignmentResponse {
    private String id;
    private String templateId;
    private String personId;
    private LocalDate startDate;
    private String status;
    private Double progress;
    private List<OnboardingAssignmentItemResponse> items;
}
