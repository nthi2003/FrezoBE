package com.frezo.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalStepDto {
    private String id;
    private Integer stepOrder;
    private String approverName;
    private String approverUsername;
    private String action;
    private String comment;
    private String actionedAt;
}
