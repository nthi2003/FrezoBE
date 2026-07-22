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
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "onboarding_assignment_item", indexes = @Index(name = "idx_ob_asg_item", columnList = "assignment_id"))
public class OnboardingAssignmentItem extends BaseEntity {

    @Column(name = "assignment_id", length = 36, nullable = false)
    private String assignmentId;

    @Column(name = "template_item_id", length = 36)
    private String templateItemId;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** PENDING / DONE / SKIPPED */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "sort_order")
    private Integer sortOrder;
}
