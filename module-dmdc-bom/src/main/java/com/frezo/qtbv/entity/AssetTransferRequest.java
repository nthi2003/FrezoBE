package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ticket workflow cấp phát / thu hồi tài sản.
 * <p>
 * Khác {@link AssetAssignment} — vốn là <b>audit log</b> đã hoàn tất — bảng này là
 * <b>ticket đang xử lý</b> có state machine:
 *
 * <pre>
 *   PENDING ─(approve)──▶ APPROVED ─(handover)─▶ HANDED_OVER  [Asset → IN_USE]
 *      │                      │
 *      │                      └─(handover với return)─▶ HANDED_OVER  [Asset → AVAILABLE]
 *      │
 *      ├─(reject)────▶ REJECTED   [terminal]
 *      └─(cancel)────▶ CANCELLED  [terminal]
 * </pre>
 *
 * Chỉ khi đi tới {@code HANDED_OVER} thì {@code Asset.status} + {@code assignedPersonId}
 * mới được cập nhật thực sự — trước đó tài sản vẫn ở trạng thái cũ.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "asset_transfer_request", indexes = {
        @Index(name = "idx_atr_asset", columnList = "asset_id"),
        @Index(name = "idx_atr_status", columnList = "status"),
        @Index(name = "idx_atr_person", columnList = "person_id"),
        @Index(name = "idx_atr_requester", columnList = "requester_username"),
})
public class AssetTransferRequest extends BaseEntity {

    @Column(name = "asset_id", length = 36, nullable = false)
    private String assetId;

    /** ASSIGN (cấp phát) hoặc RETURN (thu hồi). */
    @Column(name = "request_type", length = 20, nullable = false)
    private String requestType;

    @Column(name = "requester_username", length = 100, nullable = false)
    private String requesterUsername;

    /** Recipient cho ASSIGN, current holder cho RETURN. */
    @Column(name = "person_id", length = 36)
    private String personId;

    /** Denormalized để hiển thị trên UI mà không cần join. */
    @Column(name = "person_name", length = 255)
    private String personName;

    @Column(name = "reason", length = 1000)
    private String reason;

    /** Ngày dự kiến bàn giao (do requester chọn). */
    @Column(name = "planned_date")
    private LocalDate plannedDate;

    /** PENDING | APPROVED | REJECTED | HANDED_OVER | CANCELLED */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    // ---- Approval ----
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approve_note", length = 1000)
    private String approveNote;

    // ---- Reject / Cancel ----
    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    // ---- Handover ----
    @Column(name = "handed_over_by", length = 100)
    private String handedOverBy;

    @Column(name = "handed_over_at")
    private LocalDateTime handedOverAt;

    @Column(name = "handover_note", length = 1000)
    private String handoverNote;

    // ---- Workflow engine integration (v2) ----
    /**
     * ID của {@code WorkflowInstance} đang lái ticket này. Nullable để backward-compat
     * với các ticket được tạo trước khi engine wire vào — flow cũ (approve → handover
     * theo status field) vẫn hoạt động khi field này null.
     */
    @Column(name = "workflow_instance_id", length = 36)
    private String workflowInstanceId;
}
