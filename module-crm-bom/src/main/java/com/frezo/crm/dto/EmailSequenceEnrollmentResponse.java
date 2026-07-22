package com.frezo.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailSequenceEnrollmentResponse {
    private String id;
    private String sequenceId;
    private String leadId;
    private Integer currentStepOrder;
    private String status;
    private String enrolledAt;
    private String lastSentAt;
}
