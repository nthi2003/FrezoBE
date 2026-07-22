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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "approval_flow_step", indexes = {
        @Index(name = "idx_appr_flow_step_flow", columnList = "flow_id")
})
public class ApprovalFlowStep extends BaseEntity {

    @Column(name = "flow_id", length = 36, nullable = false)
    private String flowId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    /** Role code (MANAGER / HR / CFO...) — khớp FE approverRole. */
    @Column(name = "approver_role", length = 50, nullable = false)
    private String approverRole;

    @Column(name = "name", length = 255)
    private String name;
}
