package com.frezo.qlns.dto.request;

import lombok.Data;

@Data
public class PerformanceReviewRequest {
    private String cycleId;
    private String personId;
    private String managerPersonId;
    private Double selfScore;
    private String selfComment;
}
