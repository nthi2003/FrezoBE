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
@Table(name = "approval_step", indexes = {
        @Index(name = "idx_appr_step_req", columnList = "request_id")
})
public class ApprovalStep extends BaseEntity {

    @Column(name = "request_id", length = 36, nullable = false)
    private String requestId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "approver_role", length = 50)
    private String approverRole;

    @Column(name = "approver_person_id", length = 36)
    private String approverPersonId;

    @Column(name = "approver_username", length = 100)
    private String approverUsername;

    @Column(name = "approver_name", length = 255)
    private String approverName;

    /** SUBMITTED / PENDING / APPROVED / REJECTED / SKIPPED */
    @Column(name = "action", length = 20, nullable = false)
    private String action;

    @Column(name = "comment", length = 2000)
    private String comment;

    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;
}
