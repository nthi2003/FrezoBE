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

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "acc_bank_statement", indexes = {
        @Index(name = "idx_bank_stmt_account", columnList = "account_id")
})
public class BankStatement extends BaseEntity {

    @Column(name = "account_id", length = 36, nullable = false)
    private String accountId;

    @Column(name = "account_code", length = 20)
    private String accountCode;

    @Column(name = "account_name", length = 255)
    private String accountName;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "imported_lines")
    private Integer importedLines;

    @Column(name = "matched_count")
    private Integer matchedCount;

    @Column(name = "unmatched_count")
    private Integer unmatchedCount;

    /** OPEN / LOCKED */
    @Column(name = "status", length = 20)
    private String status;
}
