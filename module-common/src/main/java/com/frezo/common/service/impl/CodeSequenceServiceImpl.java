package com.frezo.common.service.impl;

import com.frezo.common.entity.CodeSequence;
import com.frezo.common.repository.CodeSequenceRepository;
import com.frezo.common.service.CodeSequenceService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        CodeSequence seq = repository.findByPrefix(prefix)
                .orElseGet(() -> {
                    CodeSequence newSeq = CodeSequence.builder()
                            .prefix(prefix)
                            .currentSeq(0L)
                            .build();
                    return repository.save(newSeq);
                });

        // Optimistic/pessimistic lock
        entityManager.lock(seq, LockModeType.PESSIMISTIC_WRITE);

        seq.setCurrentSeq(seq.getCurrentSeq() + 1);
        repository.save(seq);

        String fmt = "%0" + digits + "d";
        return prefix + "-" + String.format(fmt, seq.getCurrentSeq());
    }

    @Override
    @Transactional
    public void initSequence(String prefix, Long startValue, String description) {
        CodeSequence seq = CodeSequence.builder()
                .prefix(prefix)
                .currentSeq(startValue)
                .description(description)
                .build();
        repository.save(seq);
    }
}
