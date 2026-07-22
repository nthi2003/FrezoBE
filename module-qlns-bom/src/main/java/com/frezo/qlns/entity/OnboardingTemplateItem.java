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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "onboarding_template_item", indexes = @Index(name = "idx_ob_tpl_item", columnList = "template_id"))
public class OnboardingTemplateItem extends BaseEntity {

    @Column(name = "template_id", length = 36, nullable = false)
    private String templateId;

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "assignee_role", length = 50)
    private String assigneeRole;

    @Column(name = "due_day_offset")
    private Integer dueDayOffset;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "required")
    private Boolean required;
}
