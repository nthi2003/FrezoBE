package com.frezo.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowStepTemplate {
    private Integer stepOrder;
    private String approverRole;
    private String label;
}
