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

import java.math.BigDecimal;

/**
 * Ứng viên (pool) — độc lập với requisition; 1 candidate có thể apply nhiều JD.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recr_candidate", indexes = {
        @Index(name = "idx_cand_email", columnList = "email"),
        @Index(name = "idx_cand_phone", columnList = "phone")
})
public class Candidate extends BaseEntity {

    @Column(name = "full_name", length = 255, nullable = false)
    private String fullName;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    /** LinkedIn / TopCV / Facebook / Referral / Website. */
    @Column(name = "source", length = 50)
    private String source;

    @Column(name = "current_position", length = 255)
    private String currentPosition;

    @Column(name = "expected_salary", precision = 18, scale = 2)
    private BigDecimal expectedSalary;

    @Column(name = "cv_url", length = 1000)
    private String cvUrl;

    @Column(name = "linked_in_url", length = 1000)
    private String linkedInUrl;

    @Column(name = "notes", length = 2000)
    private String notes;
}
