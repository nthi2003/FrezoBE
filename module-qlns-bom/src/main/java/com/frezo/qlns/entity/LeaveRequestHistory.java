package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Audit trail cho workflow duyệt đơn nghỉ phép.
 * <p>
 * Mỗi transition (submit/approve/reject/cancel/auto-route) sinh 1 record.
 * Cho phép hiển thị timeline "ai làm gì khi nào" ở FE drawer + explain
 * cho HR/manager tại sao đơn bị chuyển sang cấp khác.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leave_request_history", indexes = {
        @Index(name = "idx_lrh_request", columnList = "request_id"),
        @Index(name = "idx_lrh_actor", columnList = "actor_username")
})
public class LeaveRequestHistory extends BaseEntity {

    @Column(name = "request_id", nullable = false, length = 36)
    private String requestId;

    /** SUBMIT | APPROVE | REJECT | CANCEL | AUTO_ROUTE | REASSIGN */
    @Column(name = "action", length = 30, nullable = false)
    private String action;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    /** Username thực hiện action. */
    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    /** Vai trò khi thao tác — REQUESTER / MANAGER / HR / SYSTEM. */
    @Column(name = "actor_role", length = 30)
    private String actorRole;

    /** Comment tuỳ chọn (lý do từ chối, ghi chú duyệt, ...). */
    @Column(name = "comment", length = 1000)
    private String comment;
}
