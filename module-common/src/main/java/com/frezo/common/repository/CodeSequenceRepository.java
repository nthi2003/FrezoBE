package com.frezo.common.repository;

import com.frezo.common.entity.CodeSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodeSequenceRepository extends JpaRepository<CodeSequence, String> {
    Optional<CodeSequence> findByPrefix(String prefix);
}
