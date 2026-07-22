package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Giai đoạn trong pipeline (Prospect → Qualified → Proposal → Negotiation → Closed).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_stage", indexes = @Index(name = "idx_crm_stage_pipeline", columnList = "pipeline_id"))
public class Stage extends BaseEntity {

    @Column(name = "pipeline_id", nullable = false, length = 36)
    private String pipelineId;

    @Column(nullable = false, length = 100)
    private String name;

    /** Thứ tự hiển thị (0, 1, 2...). */
    @Column(name = "order_no", nullable = false)
    private Integer orderNo;

    /** Tỉ lệ chốt kỳ vọng của giai đoạn (0-100). */
    @Column(name = "probability")
    private Integer probability;

    /** true = giai đoạn "Won", false = "Lost", null = trong pipeline. */
    @Column(name = "won")
    private Boolean won;
}
