package com.frezo.crm.repository;

import com.frezo.crm.entity.EmailSequenceStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailSequenceStepRepository extends JpaRepository<EmailSequenceStep, String> {
    List<EmailSequenceStep> findBySequenceIdAndIsDeletedFalseOrderByStepOrderAsc(String sequenceId);
}
