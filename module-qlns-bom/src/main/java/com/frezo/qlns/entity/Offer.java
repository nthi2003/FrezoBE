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
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thư mời làm việc — gắn với 1 {@link JobApplication} đã tới stage OFFER.
 * <p>Status: {@code DRAFT → SENT → (ACCEPTED | REJECTED | EXPIRED)}.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recr_offer", indexes = {
        @Index(name = "idx_offer_app", columnList = "application_id"),
        @Index(name = "idx_offer_status", columnList = "status")
})
public class Offer extends BaseEntity {

    @Column(name = "application_id", length = 36, nullable = false)
    private String applicationId;

    @Column(name = "offered_salary", precision = 18, scale = 2, nullable = false)
    private BigDecimal offeredSalary;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /** DRAFT / SENT / ACCEPTED / REJECTED / EXPIRED. */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "notes", length = 2000)
    private String notes;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
