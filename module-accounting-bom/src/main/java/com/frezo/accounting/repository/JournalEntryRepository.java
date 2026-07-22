package com.frezo.accounting.repository;

import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.entity.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntry, String> {

    Optional<JournalEntry> findByIdempotencyKey(String idempotencyKey);

    Optional<JournalEntry> findByCode(String code);

    List<JournalEntry> findByPeriodIdOrderByPostingDateDesc(String periodId);

    List<JournalEntry> findBySourceTypeAndSourceIdOrderByPostingDateDesc(PostingSource sourceType, String sourceId);

    List<JournalEntry> findByPostingDateBetweenOrderByPostingDateAsc(LocalDate from, LocalDate to);
}
