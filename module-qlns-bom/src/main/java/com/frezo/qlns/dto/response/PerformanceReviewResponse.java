package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReviewResponse {
    private String id;
    private String cycleId;
    private String personId;
    private String managerPersonId;
    private Double selfScore;
    private Double managerScore;
    private String selfComment;
    private String managerComment;
    private String status;
    private String submittedAt;
    private String scoredAt;
}
