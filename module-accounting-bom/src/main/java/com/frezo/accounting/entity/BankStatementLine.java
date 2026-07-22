package com.frezo.accounting.entity;

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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "acc_bank_statement_line", indexes = {
        @Index(name = "idx_bank_line_stmt", columnList = "statement_id"),
        @Index(name = "idx_bank_line_status", columnList = "match_status")
})
public class BankStatementLine extends BaseEntity {

    @Column(name = "statement_id", length = 36, nullable = false)
    private String statementId;

    @Column(name = "txn_date", nullable = false)
    private LocalDate txnDate;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "ref_code", length = 100)
    private String refCode;

    @Column(name = "debit", precision = 20, scale = 2)
    private BigDecimal debit;

    @Column(name = "credit", precision = 20, scale = 2)
    private BigDecimal credit;

    @Column(name = "balance", precision = 20, scale = 2)
    private BigDecimal balance;

    /** UNMATCHED / MATCHED / SUGGESTED */
    @Column(name = "match_status", length = 20, nullable = false)
    private String matchStatus;

    @Column(name = "matched_journal_line_id", length = 36)
    private String matchedJournalLineId;
}
