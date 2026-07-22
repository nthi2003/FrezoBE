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

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "crm_email_sequence_enrollment", indexes = {
        @Index(name = "idx_enroll_seq", columnList = "sequence_id"),
        @Index(name = "idx_enroll_lead", columnList = "lead_id")
})
public class EmailSequenceEnrollment extends BaseEntity {

    @Column(name = "sequence_id", length = 36, nullable = false)
    private String sequenceId;

    @Column(name = "lead_id", length = 36, nullable = false)
    private String leadId;

    @Column(name = "current_step_order")
    private Integer currentStepOrder;

    /** ACTIVE / COMPLETED / PAUSED / CANCELLED */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Column(name = "last_sent_at")
    private LocalDateTime lastSentAt;
}
