package com.frezo.common.workflow.repository;

import com.frezo.common.workflow.entity.WorkflowStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, String> {

    List<WorkflowStep> findByDefinitionIdAndIsDeletedFalseOrderByStepOrderAsc(String definitionId);

    void deleteByDefinitionId(String definitionId);
}
