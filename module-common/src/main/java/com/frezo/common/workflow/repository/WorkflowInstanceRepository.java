package com.frezo.common.workflow.repository;

import com.frezo.common.workflow.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, String> {

    /** Instance đang RUNNING/COMPLETED/... của 1 entity. Mỗi entity có tối đa 1 tại 1 thời điểm. */
    Optional<WorkflowInstance> findFirstByEntityTypeAndEntityIdOrderByStartedAtDesc(String entityType, String entityId);
}
