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

    /**
     * Trạng thái workflow:
     * <ul>
     *   <li>{@code PENDING_MANAGER} — chờ QL trực tiếp duyệt</li>
     *   <li>{@code PENDING_HR}      — QL đã duyệt, chờ HR chốt</li>
     *   <li>{@code APPROVED}         — HR đã duyệt, đơn có hiệu lực</li>
     *   <li>{@code REJECTED}         — bị từ chối ở bất kỳ cấp nào</li>
     *   <li>{@code CANCELLED}        — người xin tự huỷ (khi còn PENDING)</li>
     * </ul>
     * Legacy value {@code PENDING} vẫn được service coi tương đương {@code PENDING_MANAGER}
     * để đảm bảo backward-compat với dữ liệu cũ.
     */
    @Column(name = "status", length = 30)
    private String status;

    /** Username của QL trực tiếp — resolve từ Department.managerId lúc create. */
    @Column(name = "manager_username", length = 100)
    private String managerUsername;

    /** URL file đính kèm — VD giấy khám bệnh cho sick leave. */
    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    // ---- QL trực tiếp duyệt ----
    @Column(name = "manager_approved_by", length = 100)
    private String managerApprovedBy;

    @Column(name = "manager_approved_at")
    private LocalDate managerApprovedAt;

    // ---- HR duyệt chốt ----
    @Column(name = "hr_approved_by", length = 100)
    private String hrApprovedBy;

    @Column(name = "hr_approved_at")
    private LocalDate hrApprovedAt;

    // ---- Legacy fields (giữ để không phá dữ liệu cũ) ----
    /** @deprecated dùng {@link #managerApprovedBy} + {@link #hrApprovedBy}. */
    @Deprecated
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /** @deprecated dùng {@link #managerApprovedAt} + {@link #hrApprovedAt}. */
    @Deprecated
    @Column(name = "approved_at")
    private LocalDate approvedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDate rejectedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** ID ApprovalRequest (module-approval) — null nếu tạo trước Sprint 2. */
    @Column(name = "approval_request_id", length = 36)
    private String approvalRequestId;
}
