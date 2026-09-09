package com.frezo.common.workflow.repository;

import com.frezo.common.workflow.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, String> {

    Optional<WorkflowInstance> findFirstByEntityTypeAndEntityIdOrderByStartedAtDesc(String entityType, String entityId);
}
