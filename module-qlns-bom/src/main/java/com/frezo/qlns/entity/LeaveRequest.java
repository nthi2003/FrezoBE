package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Đơn xin nghỉ phép
 * - contractId / personId: nhân viên
 * - leaveType: annual / sick / unpaid / other
 * - startDate / endDate / durationDays
 * - status: PENDING / APPROVED / REJECTED / CANCELLED
 * - approvedBy / rejectedBy: người duyệt/từ chối
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leave_request")
public class LeaveRequest extends BaseEntity {

    @Column(name = "contract_id", nullable = false)
    private String contractId;

    @Column(name = "person_id")
    private String personId;

    /** annual / sick / unpaid / other */
    @Column(name = "leave_type", length = 30, nullable = false)
    private String leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Số ngày nghỉ */
    @Column(name = "duration_days")
    private Double durationDays;

    @Column(name = "reason", length = 1000)
    private String reason;

    /** PENDING / APPROVED / REJECTED / CANCELLED */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDate approvedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
}
