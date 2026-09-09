package com.frezo.common.workflow.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 1 bước trong quy trình duyệt. Được ràng buộc theo thứ tự {@code stepOrder}.
 * <p>
 * Approver được xác định qua {@code approverType} + {@code approverValue}:
 * <ul>
 *   <li>{@code USER}    — {@code approverValue} = username (VD "hr01")</li>
 *   <li>{@code ROLE}    — {@code approverValue} = role code (VD "HR", "ASSET_MANAGER")
 *       — bất kỳ user nào có role đó đều có thể duyệt (task pool)</li>
 *   <li>{@code MANAGER} — approver = quản lý trực tiếp của requester (resolver runtime,
 *       cần đăng ký {@link com.frezo.common.workflow.spi.ApproverResolver} beans)</li>
 *   <li>{@code ADMIN}   — bất kỳ admin nào (Person.isAdmin=true)</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "workflow_step", indexes = {
        @Index(name = "idx_wf_step_def", columnList = "definition_id"),
})
public class WorkflowStep extends BaseEntity {

    @Column(name = "definition_id", length = 36, nullable = false)
    private String definitionId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Column(name = "step_name", length = 255, nullable = false)
    private String stepName;


    @Column(name = "approver_type", length = 30, nullable = false)
    private String approverType;


    @Column(name = "approver_value", length = 255)
    private String approverValue;


    @Column(name = "allow_skip", nullable = false)
    private Boolean allowSkip;


    @Column(name = "sla_hours")
    private Integer slaHours;


    @Column(name = "description", length = 500)
    private String description;
}
