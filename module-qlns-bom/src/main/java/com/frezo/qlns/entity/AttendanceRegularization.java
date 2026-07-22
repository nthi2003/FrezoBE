package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Đơn giải trình chấm công — nhân viên tạo khi quên check-in/out,
 * chấm sai giờ, hoặc bị lỗi kỹ thuật. Workflow 1 tầng (manager duyệt).
 * <p>
 * status: PENDING → APPROVED / REJECTED. Nếu APPROVED thì service tự cập nhật
 * vào Attendance record của ngày tương ứng (tạo mới nếu chưa có).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attendance_regularization")
public class AttendanceRegularization extends BaseEntity {

    @Column(name = "person_id", nullable = false)
    private String personId;

    @Column(name = "contract_id")
    private String contractId;

    /** Ngày cần giải trình. */
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    /** Giờ check-in được xin ghi nhận (nullable — nếu chỉ giải trình checkout). */
    @Column(name = "requested_check_in")
    private LocalTime requestedCheckIn;

    /** Giờ check-out được xin ghi nhận. */
    @Column(name = "requested_check_out")
    private LocalTime requestedCheckOut;

    @Column(name = "reason", length = 1000, nullable = false)
    private String reason;

    /** PENDING / APPROVED / REJECTED */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "manager_username", length = 100)
    private String managerUsername;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
}
