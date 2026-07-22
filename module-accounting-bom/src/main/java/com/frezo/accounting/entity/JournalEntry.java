package com.frezo.accounting.entity;

import com.frezo.accounting.common.JournalStatus;
import com.frezo.accounting.common.PostingSource;
import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Chứng từ ghi sổ (Journal Entry). Mỗi chứng từ có nhiều {@link JournalEntryLine}
 * — tổng Debit của các line = tổng Credit (double-entry).
 * <p>Trạng thái: DRAFT → POSTED. Khi cần huỷ posting, tạo một chứng từ REVERSAL
 * đảo lại thay vì xoá — đảm bảo audit trail nguyên vẹn.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "acc_journal_entry",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_journal_entry_code", columnNames = {"code"}),
        indexes = {
                @Index(name = "idx_acc_je_period", columnList = "period_id"),
                @Index(name = "idx_acc_je_source", columnList = "source_type,source_id"),
                @Index(name = "idx_acc_je_date", columnList = "posting_date")
        })
public class JournalEntry extends BaseEntity {

    /** Số chứng từ (unique). VD: "JV-2026-01-000123". */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /** Ngày ghi sổ (dùng để xác định period). */
    @Column(name = "posting_date", nullable = false)
    private LocalDate postingDate;

    /** Ngày chứng từ (documentDate, có thể khác postingDate). */
    @Column(name = "document_date")
    private LocalDate documentDate;

    @Column(name = "period_id", nullable = false, length = 36)
    private String periodId;

    /** Diễn giải chung. */
    @Column(name = "description", nullable = false, length = 500)
    private String description;

    /** Nguồn phát sinh — MANUAL, PAYROLL, INVOICE, GRN... */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private PostingSource sourceType;

    /** ID của document nguồn (payroll_period_id, invoice_id...). */
    @Column(name = "source_id", length = 36)
    private String sourceId;

    /** Idempotency key — chống double-post. */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private JournalStatus status;

    /** Tổng debit (phải = totalCredit khi POSTED). */
    @Column(name = "total_debit", precision = 20, scale = 2, nullable = false)
    private BigDecimal totalDebit;

    @Column(name = "total_credit", precision = 20, scale = 2, nullable = false)
    private BigDecimal totalCredit;

    @Column(name = "posted_at")
    private java.time.LocalDateTime postedAt;

    @Column(name = "posted_by", length = 50)
    private String postedBy;

    /** ID chứng từ đảo (khi entry này là REVERSAL). */
    @Column(name = "reversal_of_id", length = 36)
    private String reversalOfId;

    /**
     * Các line chi tiết (Nợ / Có).
     * <p>KHÔNG là JPA mapping — chỉ dùng khi service load kèm để trả DTO ra ngoài.
     * Line được query trực tiếp qua {@code JournalEntryLineRepository.findByJournalEntryId(...)}.
     */
    @Transient
    @Builder.Default
    private List<JournalEntryLine> lines = new ArrayList<>();
}
