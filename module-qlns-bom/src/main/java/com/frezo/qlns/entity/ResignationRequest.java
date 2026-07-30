package com.frezo.qlns.entity;

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

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Đơn nghỉ việc — workflow offboarding 5 bước.
 * <p>
 * Status: REQUESTED → APPROVED → HANDOVER_DONE → PAYROLL_SETTLED → COMPLETED | CANCELLED
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "resignation_request", indexes = {
        @Index(name = "idx_resignation_person", columnList = "person_id"),
        @Index(name = "idx_resignation_status", columnList = "status")
})
public class ResignationRequest extends BaseEntity {

    @Column(name = "request_code", nullable = false, length = 50)
    private String requestCode;

    @Column(name = "person_id", nullable = false, length = 36)
    private String personId;

    @Column(name = "person_name", length = 500)
    private String personName;

    @Column(name = "expected_last_day")
    private LocalDate expectedLastDay;

    @Column(name = "actual_last_day")
    private LocalDate actualLastDay;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /** REQUESTED / APPROVED / HANDOVER_DONE / PAYROLL_SETTLED / COMPLETED / CANCELLED */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "manager_approved_by", length = 100)
    private String managerApprovedBy;

    @Column(name = "manager_approved_at")
    private LocalDateTime managerApprovedAt;

    @Column(name = "hr_confirmed_by", length = 100)
    private String hrConfirmedBy;

    @Column(name = "hr_confirmed_at")
    private LocalDateTime hrConfirmedAt;

    @Column(name = "laptop_returned")
    private Boolean laptopReturned;

    @Column(name = "badge_returned")
    private Boolean badgeReturned;

    @Column(name = "docs_handed_over")
    private Boolean docsHandedOver;

    @Column(name = "handover_note", columnDefinition = "TEXT")
    private String handoverNote;

    @Column(name = "handover_at")
    private LocalDateTime handoverAt;

    @Column(name = "payroll_settled_at")
    private LocalDateTime payrollSettledAt;

    @Column(name = "user_revoked_at")
    private LocalDateTime userRevokedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
