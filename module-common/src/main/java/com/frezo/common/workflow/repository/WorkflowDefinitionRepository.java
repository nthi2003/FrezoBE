package com.frezo.common.workflow.repository;

import com.frezo.common.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, String> {

    Optional<WorkflowDefinition> findByCode(String code);

    boolean existsByCode(String code);

    /** Lấy tất cả active definitions cho 1 module — cho FE picker khi start instance. */
    List<WorkflowDefinition> findByModuleCodeAndActiveTrueAndIsDeletedFalseOrderByCreatedDateDesc(String moduleCode);

    List<WorkflowDefinition> findByIsDeletedFalseOrderByModuleCodeAscCreatedDateDesc();

    List<WorkflowDefinition> findByIsTemplateTrueAndIsDeletedFalseOrderByModuleCodeAscNameAsc();

    Optional<WorkflowDefinition> findByTemplateKeyAndIsTemplateTrueAndIsDeletedFalse(String templateKey);

    Optional<WorkflowDefinition> findByCodeAndIsTemplateTrueAndIsDeletedFalse(String code);
}
