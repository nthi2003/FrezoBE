package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Lịch sử post khấu hao 1 kỳ (month/year) — 1 dòng / kỳ (không phải mỗi asset).
 * <p>{@code journalEntryId} là ID chứng từ trong module Accounting đã post GL,
 * dùng để đối soát và cho phép reverse nếu cần.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "depreciation_posting",
        uniqueConstraints = @UniqueConstraint(name = "uk_dep_post_period",
                columnNames = {"period_year", "period_month"}),
        indexes = @Index(name = "idx_dep_post_status", columnList = "status"))
public class DepreciationPosting extends BaseEntity {

    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    @Column(name = "period_month", nullable = false)
    private Integer periodMonth;

    /** Tổng khấu hao kỳ (sum monthlyAmount của các schedule ACTIVE). */
    @Column(name = "total_amount", precision = 18, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** Số schedule tham gia — audit trail. */
    @Column(name = "schedule_count")
    private Integer scheduleCount;

    /** ID chứng từ GL do JournalService.createAndPost() trả về. */
    @Column(name = "journal_entry_id", length = 36)
    private String journalEntryId;

    /** POSTED / REVERSED / FAILED. */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;
}
