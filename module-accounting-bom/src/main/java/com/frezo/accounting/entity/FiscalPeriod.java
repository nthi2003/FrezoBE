package com.frezo.accounting.entity;

import com.frezo.accounting.common.PeriodStatus;
import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Kỳ kế toán (tháng). Mỗi năm có 12 kỳ. Journal entry chỉ post được vào kỳ OPEN.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "acc_fiscal_period",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_fiscal_period_month_year",
                columnNames = {"month", "year"}),
        indexes = @Index(name = "idx_acc_fiscal_period_year", columnList = "fiscal_year_id"))
public class FiscalPeriod extends BaseEntity {

    @Column(name = "fiscal_year_id", nullable = false, length = 36)
    private String fiscalYearId;

    /** Tháng (1..12). */
    @Column(name = "month", nullable = false)
    private Integer month;

    /** Năm (2026...). */
    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PeriodStatus status;

    /** Thời điểm khoá (nullable). */
    @Column(name = "closed_at")
    private java.time.LocalDateTime closedAt;

    @Column(name = "closed_by", length = 50)
    private String closedBy;
}
