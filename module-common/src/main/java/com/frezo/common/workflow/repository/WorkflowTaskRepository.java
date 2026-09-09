package com.frezo.common.workflow.repository;

import com.frezo.common.workflow.entity.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, String> {


    List<WorkflowTask> findByInstanceIdOrderByStepOrderAsc(String instanceId);


    @Query(
        "SELECT t FROM WorkflowTask t WHERE t.status = 'PENDING' " +
        "AND (t.isDeleted IS NULL OR t.isDeleted = false) " +
        "AND (t.assigneeUsername = :username " +
        "     OR (t.assigneeUsername IS NULL AND t.assigneeRole IN :roles) " +
        "     OR (t.assigneeUsername IS NULL AND t.assigneeRole IS NULL AND :isAdmin = true)) " +
        "ORDER BY t.createdDate DESC"
    )
    List<WorkflowTask> findPendingForUser(String username, List<String> roles, boolean isAdmin);
}
