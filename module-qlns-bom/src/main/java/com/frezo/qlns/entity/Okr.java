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
@Table(name = "okr", indexes = {
        @Index(name = "idx_okr_owner", columnList = "owner_person_id"),
        @Index(name = "idx_okr_status", columnList = "status")
})
public class Okr extends BaseEntity {

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "owner_person_id", length = 36, nullable = false)
    private String ownerPersonId;

    @Column(name = "period_label", length = 50)
    private String periodLabel;

    @Column(name = "cycle_id", length = 36)
    private String cycleId;

    @Column(name = "department_id", length = 36)
    private String departmentId;

    @Column(name = "org_id", length = 36)
    private String orgId;

    @Column(name = "parent_okr_id", length = 36)
    private String parentOkrId;

    /** PERSONAL / TEAM / DEPARTMENT / COMPANY */
    @Column(name = "scope_type", length = 20)
    private String scopeType;

    /** COMMITTED / STRETCH */
    @Column(name = "objective_type", length = 20)
    private String objectiveType;

    @Column(name = "cross_link_ids", columnDefinition = "text")
    private String crossLinkIds;

    @Column(name = "published", nullable = false)
    private Boolean published;

    @Column(name = "published_at")
    private java.time.LocalDateTime publishedAt;

    @Column(name = "published_by", length = 100)
    private String publishedBy;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** DRAFT / ACTIVE / COMPLETED / CANCELLED */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /** 0–100, tính từ key results. */
    @Column(name = "progress")
    private Double progress;
}
