package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Đơn ứng tuyển (candidate × requisition). Đặt tên {@code JobApplication} để tránh xung đột
 * với {@code javax.security.auth.login.LoginContext.Application} và giữ tên bảng ngắn gọn.
 * <p>Stage flow: {@code APPLIED → SCREENING → INTERVIEW → OFFER → HIRED | REJECTED}.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recr_application",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_cand_req",
                columnNames = {"candidate_id", "requisition_id"}),
        indexes = {
                @Index(name = "idx_app_stage", columnList = "stage"),
                @Index(name = "idx_app_req", columnList = "requisition_id")
        })
public class JobApplication extends BaseEntity {

    @Column(name = "candidate_id", length = 36, nullable = false)
    private String candidateId;

    @Column(name = "requisition_id", length = 36, nullable = false)
    private String requisitionId;

    /** APPLIED / SCREENING / INTERVIEW / OFFER / HIRED / REJECTED. */
    @Column(name = "stage", length = 20, nullable = false)
    private String stage;

    @Column(name = "applied_date", nullable = false)
    private LocalDate appliedDate;

    /** Username phụ trách stage hiện tại (HR / hiring manager / interviewer). */
    @Column(name = "current_assignee", length = 100)
    private String currentAssignee;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;
}
