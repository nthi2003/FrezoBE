package com.frezo.common.workflow.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Task duyệt cụ thể của 1 bước trong 1 instance.
 * <p>
 * Cơ chế "pool":
 * <ul>
 *   <li>Approver type USER → {@code assigneeUsername} = username (chỉ user đó duyệt)</li>
 *   <li>Approver type ROLE/MANAGER/ADMIN → {@code assigneeRole} = role code, {@code assigneeUsername} = null
 *       → bất kỳ user thoả role đều thấy task này trong inbox và có thể "grab" duyệt</li>
 * </ul>
 * <p>
 * Khi 1 task được duyệt → engine advance instance sang step tiếp theo và tạo task mới.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "workflow_task", indexes = {
        @Index(name = "idx_wf_task_instance", columnList = "instance_id"),
        @Index(name = "idx_wf_task_assignee", columnList = "assignee_username"),
        @Index(name = "idx_wf_task_role", columnList = "assignee_role"),
        @Index(name = "idx_wf_task_status", columnList = "status"),
})
public class WorkflowTask extends BaseEntity {

    @Column(name = "instance_id", length = 36, nullable = false)
    private String instanceId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_name", length = 255, nullable = false)
    private String stepName;

    /** USER | ROLE | MANAGER | ADMIN — snapshot lúc tạo task. */
    @Column(name = "approver_type", length = 30, nullable = false)
    private String approverType;

    /** Nullable — cho role/admin pool. */
    @Column(name = "assignee_username", length = 100)
    private String assigneeUsername;

    /** Nullable — role code khi type=ROLE. */
    @Column(name = "assignee_role", length = 100)
    private String assigneeRole;

    /** PENDING | APPROVED | REJECTED | SKIPPED */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "decided_by", length = 100)
    private String decidedBy;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Column(name = "comment", length = 1000)
    private String comment;

    /** SLA deadline (nếu step có slaHours). Null = không tính. */
    @Column(name = "deadline")
    private LocalDateTime deadline;
}
