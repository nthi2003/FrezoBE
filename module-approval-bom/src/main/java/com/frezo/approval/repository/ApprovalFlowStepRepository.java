package com.frezo.approval.repository;

import com.frezo.approval.entity.ApprovalFlowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalFlowStepRepository extends JpaRepository<ApprovalFlowStep, String> {

    List<ApprovalFlowStep> findByFlowIdAndIsDeletedFalseOrderByStepOrderAsc(String flowId);

    void deleteByFlowId(String flowId);
}
