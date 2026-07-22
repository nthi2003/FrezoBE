package com.frezo.approval.repository;

import com.frezo.approval.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, String> {

    List<ApprovalRequest> findByStatusAndIsDeletedFalseOrderByRequestedAtDesc(String status);

    List<ApprovalRequest> findByIsDeletedFalseOrderByRequestedAtDesc();

    Optional<ApprovalRequest> findFirstBySubjectTypeAndSubjectIdAndIsDeletedFalseOrderByRequestedAtDesc(
            String subjectType, String subjectId);
}
