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

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "onboarding_assignment", indexes = {
        @Index(name = "idx_ob_assign_person", columnList = "person_id")
})
public class OnboardingAssignment extends BaseEntity {

    @Column(name = "template_id", length = 36, nullable = false)
    private String templateId;

    @Column(name = "person_id", length = 36, nullable = false)
    private String personId;

    @Column(name = "start_date")
    private LocalDate startDate;

    /** IN_PROGRESS / COMPLETED / CANCELLED */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "progress")
    private Double progress;
}
