package com.frezo.approval.repository;

import com.frezo.approval.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, String> {

    List<ApprovalStep> findByRequestIdAndIsDeletedFalseOrderByStepOrderAsc(String requestId);

    Optional<ApprovalStep> findByRequestIdAndStepOrderAndIsDeletedFalse(String requestId, Integer stepOrder);
}
