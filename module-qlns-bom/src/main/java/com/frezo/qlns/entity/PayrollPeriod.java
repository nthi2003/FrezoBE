package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "payroll_period")
public class PayrollPeriod extends BaseEntity {

    @Column(name = "org_id")
    private String orgId;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", length = 50)
    private String lockedBy;

    @Column(name = "note", length = 500)
    private String note;

    /**
     * ID {@code WorkflowInstance} đang lái kỳ lương này. Nullable để backward-compat:
     * <ul>
     *   <li>Null → chưa có workflow (kỳ tạo trước khi engine wire vào, hoặc chưa lock)</li>
     *   <li>!= null → đang duyệt qua Workflow Engine (theo definition {@code PAYROLL_DEFAULT}
     *       hoặc admin chọn khác). Approve/reject sẽ đi qua {@code WorkflowService} thay
     *       vì set status trực tiếp.</li>
     * </ul>
     */
    @Column(name = "workflow_instance_id", length = 36)
    private String workflowInstanceId;

    /** ID ApprovalRequest (module-approval) — thay dần workflow_instance_id. */
    @Column(name = "approval_request_id", length = 36)
    private String approvalRequestId;
}
