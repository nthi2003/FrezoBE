package com.frezo.crm.repository;

import com.frezo.crm.entity.EmailSequence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailSequenceRepository extends JpaRepository<EmailSequence, String> {
    List<EmailSequence> findByIsDeletedFalseOrderByCreatedDateDesc();
}
