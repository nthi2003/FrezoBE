package com.frezo.qlns.repository;

import com.frezo.qlns.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CandidateRepository extends JpaRepository<Candidate, String>,
        JpaSpecificationExecutor<Candidate> {
}
