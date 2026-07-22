package com.frezo.approval.entity;

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
@Table(name = "approval_flow", indexes = {
        @Index(name = "idx_appr_flow_subject", columnList = "subject_type")
})
public class ApprovalFlow extends BaseEntity {

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "subject_type", length = 40, nullable = false)
    private String subjectType;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "description", length = 1000)
    private String description;
}
