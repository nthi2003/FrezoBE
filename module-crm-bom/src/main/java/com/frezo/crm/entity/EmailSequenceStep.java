package com.frezo.crm.entity;

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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "crm_email_sequence_step", indexes = @Index(name = "idx_seq_step", columnList = "sequence_id"))
public class EmailSequenceStep extends BaseEntity {

    @Column(name = "sequence_id", length = 36, nullable = false)
    private String sequenceId;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    /** Delay ngày sau enroll / bước trước. */
    @Column(name = "delay_days")
    private Integer delayDays;

    @Column(name = "subject", length = 500)
    private String subject;

    @Column(name = "body_html", length = 4000)
    private String bodyHtml;
}
