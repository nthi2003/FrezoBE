package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


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
    @Column(name = "request_type", length = 20, nullable = false)
    private String requestType;
    @Column(name = "requester_username", length = 100, nullable = false)
    private String requesterUsername;
    @Column(name = "person_id", length = 36)
    private String personId;
    @Column(name = "person_name", length = 255)
    private String personName;
    @Column(name = "reason", length = 1000)
    private String reason;
    @Column(name = "planned_date")
    private LocalDate plannedDate;
    @Column(name = "status", length = 30, nullable = false)
    private String status;
    @Column(name = "approved_by", length = 100)
    private String approvedBy;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "approve_note", length = 1000)
    private String approveNote;
    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    @Column(name = "reject_reason", length = 1000)
    private String rejectReason;
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    @Column(name = "handed_over_by", length = 100)
    private String handedOverBy;
    @Column(name = "handed_over_at")
    private LocalDateTime handedOverAt;
    @Column(name = "handover_note", length = 1000)
    private String handoverNote;
    @Column(name = "workflow_instance_id", length = 36)
    private String workflowInstanceId;
}
