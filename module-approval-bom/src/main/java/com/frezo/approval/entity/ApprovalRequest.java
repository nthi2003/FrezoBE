package com.frezo.approval.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "approval_request", indexes = {
        @Index(name = "idx_appr_req_status", columnList = "status"),
        @Index(name = "idx_appr_req_subject", columnList = "subject_type,subject_id")
})
public class ApprovalRequest extends BaseEntity {

    @Column(name = "flow_id", length = 36)
    private String flowId;

    @Column(name = "subject_type", length = 40, nullable = false)
    private String subjectType;

    @Column(name = "subject_id", length = 36, nullable = false)
    private String subjectId;

    @Column(name = "subject_summary", length = 500)
    private String subjectSummary;

    @Column(name = "requested_by", length = 100, nullable = false)
    private String requestedBy;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "current_step_order")
    private Integer currentStepOrder;

    @Column(name = "total_steps")
    private Integer totalSteps;

    /** PENDING / APPROVED / REJECTED / CANCELLED */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "current_approver_hint", length = 100)
    private String currentApproverHint;
}
