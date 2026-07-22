package com.frezo.accounting.repository;

import com.frezo.accounting.entity.JournalEntryLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface JournalEntryLineRepository extends JpaRepository<JournalEntryLine, String> {

    List<JournalEntryLine> findByJournalEntryIdOrderByLineNoAsc(String journalEntryId);

    List<JournalEntryLine> findByAccountIdOrderByLineNoAsc(String accountId);

    /**
     * Truy vấn sổ cái — lấy tất cả line của tài khoản trong khoảng thời gian.
     * Chỉ tính line từ chứng từ đã POSTED.
     */
    @Query("""
        SELECT l FROM JournalEntryLine l
        JOIN JournalEntry j ON j.id = l.journalEntryId
        WHERE l.accountId = :accountId
          AND j.status = com.frezo.accounting.common.JournalStatus.POSTED
          AND j.postingDate BETWEEN :from AND :to
        ORDER BY j.postingDate, j.code, l.lineNo
    """)
    List<JournalEntryLine> findGLLines(@Param("accountId") String accountId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    /** Tổng debit của TK từ khi có sổ đến hết ngày `toDate` (cho tính số dư đầu kỳ). */
    @Query("""
        SELECT COALESCE(SUM(l.debit), 0) FROM JournalEntryLine l
        JOIN JournalEntry j ON j.id = l.journalEntryId
        WHERE l.accountId = :accountId
          AND j.status = com.frezo.accounting.common.JournalStatus.POSTED
          AND j.postingDate <= :toDate
    """)
    BigDecimal sumDebitToDate(@Param("accountId") String accountId, @Param("toDate") LocalDate toDate);

    @Query("""
        SELECT COALESCE(SUM(l.credit), 0) FROM JournalEntryLine l
        JOIN JournalEntry j ON j.id = l.journalEntryId
        WHERE l.accountId = :accountId
          AND j.status = com.frezo.accounting.common.JournalStatus.POSTED
          AND j.postingDate <= :toDate
    """)
    BigDecimal sumCreditToDate(@Param("accountId") String accountId, @Param("toDate") LocalDate toDate);

    /**
     * Gợi ý match bank reconciliation — exact amount + cùng ngày trên TK ngân hàng.
     */
    @Query("""
        SELECT l FROM JournalEntryLine l
        JOIN JournalEntry j ON j.id = l.journalEntryId
        WHERE l.accountId = :accountId
          AND j.status = com.frezo.accounting.common.JournalStatus.POSTED
          AND j.postingDate = :txnDate
          AND (
            (:amount > 0 AND (l.debit = :amount OR l.credit = :amount))
            OR (:amount = 0)
          )
        ORDER BY j.code, l.lineNo
    """)
    List<JournalEntryLine> findMatchCandidates(
            @Param("accountId") String accountId,
            @Param("txnDate") LocalDate txnDate,
            @Param("amount") BigDecimal amount);

    /** Candidates trong khoảng ngày (fuzzy). */
    @Query("""
        SELECT l FROM JournalEntryLine l
        JOIN JournalEntry j ON j.id = l.journalEntryId
        WHERE l.accountId = :accountId
          AND j.status = com.frezo.accounting.common.JournalStatus.POSTED
          AND j.postingDate BETWEEN :from AND :to
        ORDER BY j.postingDate DESC, j.code, l.lineNo
    """)
    List<JournalEntryLine> findFuzzyCandidates(
            @Param("accountId") String accountId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    /** Trial balance — tổng debit / credit của mọi account trong khoảng thời gian. */
    @Query("""
        SELECT l.accountId, l.accountCode,
               COALESCE(SUM(l.debit), 0), COALESCE(SUM(l.credit), 0)
        FROM JournalEntryLine l
        JOIN JournalEntry j ON j.id = l.journalEntryId
        WHERE j.status = com.frezo.accounting.common.JournalStatus.POSTED
          AND j.postingDate BETWEEN :from AND :to
        GROUP BY l.accountId, l.accountCode
        ORDER BY l.accountCode
    """)
    List<Object[]> aggregateByAccount(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
