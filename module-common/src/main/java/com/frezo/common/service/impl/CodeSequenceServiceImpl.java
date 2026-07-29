package com.frezo.common.service.impl;

import com.frezo.common.entity.CodeSequence;
import com.frezo.common.repository.CodeSequenceRepository;
import com.frezo.common.service.CodeSequenceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thread-safe code generator backed by {@code code_sequences}.
 * <p>
 * Uses PostgreSQL {@code INSERT … ON CONFLICT DO UPDATE … RETURNING} so the
 * first use of a prefix never hits Hibernate's false
 * {@code OptimisticLockException} from {@code lock(PESSIMISTIC_WRITE)} on a
 * row that was not flushed yet (HHH-13659) — which surfaced as HTTP 409
 * {@code error.concurrent.modification} on POST /qtbv/articles.
 */
@Service
@RequiredArgsConstructor
public class CodeSequenceServiceImpl implements CodeSequenceService {

    private final CodeSequenceRepository repository;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public String nextCode(String prefix) {
        return nextCode(prefix, 4);
    }

    @Override
    @Transactional
    public String nextCode(String prefix, int digits) {
        long next = incrementAndGet(prefix);
        String fmt = "%0" + digits + "d";
        return prefix + "-" + String.format(fmt, next);
    }

    /**
     * Atomically ensure the prefix row exists and bump {@code current_seq}.
     * Concurrent callers serialize on the row lock taken by UPSERT.
     */
    private long incrementAndGet(String prefix) {
        Query query = entityManager.createNativeQuery(
                """
                INSERT INTO code_sequences (prefix, current_seq)
                VALUES (?1, 1)
                ON CONFLICT (prefix) DO UPDATE
                  SET current_seq = code_sequences.current_seq + 1
                RETURNING current_seq
                """);
        query.setParameter(1, prefix);
        Object result = query.getSingleResult();
        return ((Number) result).longValue();
    }

    @Override
    @Transactional
    public void initSequence(String prefix, Long startValue, String description) {
        if (repository.findByPrefix(prefix).isPresent()) {
            return;
        }
        repository.saveAndFlush(CodeSequence.builder()
                .prefix(prefix)
                .currentSeq(startValue != null ? startValue : 0L)
                .description(description)
                .build());
    }
}
