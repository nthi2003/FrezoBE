package com.frezo.qlns.entity;

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
@Table(name = "performance_review", indexes = {
        @Index(name = "idx_perf_cycle", columnList = "cycle_id"),
        @Index(name = "idx_perf_person", columnList = "person_id")
})
public class PerformanceReview extends BaseEntity {

    @Column(name = "cycle_id", length = 36, nullable = false)
    private String cycleId;

    @Column(name = "person_id", length = 36, nullable = false)
    private String personId;

    @Column(name = "manager_person_id", length = 36)
    private String managerPersonId;

    @Column(name = "self_score")
    private Double selfScore;

    @Column(name = "manager_score")
    private Double managerScore;

    @Column(name = "self_comment", length = 2000)
    private String selfComment;

    @Column(name = "manager_comment", length = 2000)
    private String managerComment;

    /** DRAFT / SUBMITTED / SCORED / CLOSED */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "scored_at")
    private LocalDateTime scoredAt;
}
