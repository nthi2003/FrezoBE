package com.frezo.common.workflow.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Instance đang chạy — 1 entity (1 asset transfer, 1 leave request, ...) sinh 1 instance.
 * <p>
 * Lookup theo cặp {@code (entityType, entityId)} — mỗi entity chỉ có tối đa 1 instance
 * đang RUNNING tại 1 thời điểm.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "workflow_instance", indexes = {
        @Index(name = "idx_wf_inst_entity", columnList = "entity_type,entity_id"),
        @Index(name = "idx_wf_inst_status", columnList = "status"),
})
public class WorkflowInstance extends BaseEntity {

    @Column(name = "definition_code", length = 100, nullable = false)
    private String definitionCode;

    /** Loại entity — VD ASSET_TRANSFER, LEAVE_REQUEST, CONTRACT. */
    @Column(name = "entity_type", length = 50, nullable = false)
    private String entityType;

    @Column(name = "entity_id", length = 36, nullable = false)
    private String entityId;

    /** Người khởi tạo — thường là requester. */
    @Column(name = "started_by", length = 100, nullable = false)
    private String startedBy;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** Bước đang xử lý (0-based). */
    @Column(name = "current_step", nullable = false)
    private Integer currentStep;

    /** RUNNING | COMPLETED | REJECTED | CANCELLED */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Ghi chú tổng thể (VD title hiển thị trong inbox: "Yêu cầu cấp phát MacBook cho Nguyễn Văn A"). */
    @Column(name = "title", length = 500)
    private String title;
}
