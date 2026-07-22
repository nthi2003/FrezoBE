package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingAssignmentItemResponse {
    private String id;
    private String title;
    private LocalDate dueDate;
    private String status;
    private String completedAt;
    private String completedBy;
    private Integer sortOrder;
}
