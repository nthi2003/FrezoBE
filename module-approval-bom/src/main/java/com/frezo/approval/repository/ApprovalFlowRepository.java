package com.frezo.approval.repository;

import com.frezo.approval.entity.ApprovalFlow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalFlowRepository extends JpaRepository<ApprovalFlow, String> {

    List<ApprovalFlow> findByIsDeletedFalseOrderByCreatedDateDesc();

    Optional<ApprovalFlow> findFirstBySubjectTypeAndActiveTrueAndIsDeletedFalse(String subjectType);

    Optional<ApprovalFlow> findByCodeAndIsDeletedFalse(String code);

    Optional<ApprovalFlow> findByCodeAndActiveTrueAndIsDeletedFalse(String code);
}
