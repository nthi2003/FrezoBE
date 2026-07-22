package com.frezo.crm.repository;

import com.frezo.crm.entity.EmailSequenceEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailSequenceEnrollmentRepository extends JpaRepository<EmailSequenceEnrollment, String> {
    List<EmailSequenceEnrollment> findByStatusAndIsDeletedFalse(String status);
    List<EmailSequenceEnrollment> findBySequenceIdAndIsDeletedFalse(String sequenceId);
}
