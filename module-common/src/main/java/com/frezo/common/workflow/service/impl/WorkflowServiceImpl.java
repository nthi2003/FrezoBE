package com.frezo.common.workflow.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.workflow.WorkflowErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.workflow.dto.WorkflowDefinitionDto;
import com.frezo.common.workflow.dto.WorkflowInstanceDto;
import com.frezo.common.workflow.dto.WorkflowStepDto;
import com.frezo.common.workflow.dto.WorkflowTaskDto;
import com.frezo.common.workflow.entity.WorkflowDefinition;
import com.frezo.common.workflow.entity.WorkflowInstance;
import com.frezo.common.workflow.entity.WorkflowStep;
import com.frezo.common.workflow.entity.WorkflowTask;
import com.frezo.common.workflow.repository.WorkflowDefinitionRepository;
import com.frezo.common.workflow.repository.WorkflowInstanceRepository;
import com.frezo.common.workflow.repository.WorkflowStepRepository;
import com.frezo.common.workflow.repository.WorkflowTaskRepository;
import com.frezo.common.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation của workflow engine.
 *
 * <h3>State machine</h3>
 * <pre>
 *   start() → tạo Instance(status=RUNNING, currentStep=0) + Task cho step 0 (PENDING)
 *   approveTask() → task=APPROVED
 *     → nếu còn step tiếp: instance.currentStep++, tạo task mới cho step đó
 *     → nếu hết step: instance.status=COMPLETED
 *   rejectTask() → task=REJECTED, instance.status=REJECTED (terminal)
 *   cancelInstance() → instance.status=CANCELLED, all pending tasks → SKIPPED
 * </pre>
 *
 * <h3>Approver resolution</h3>
 * Với step type={@code USER} → task.assigneeUsername = step.approverValue (single approver).
 * Với {@code ROLE} → task.assigneeUsername = null, task.assigneeRole = step.approverValue
 * (pool — bất kỳ user có role đó đều thấy task trong inbox).
 * Với {@code ADMIN} → task.assigneeUsername = null, task.assigneeRole = null → mọi admin
 * thấy trong inbox (query trong repository filter theo :isAdmin).
 * {@code MANAGER} tạm thời fallback về ADMIN (chưa có org resolver runtime).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    // ---- Constants ----
    public static final String STATUS_RUNNING   = "RUNNING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_REJECTED  = "REJECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String TASK_PENDING  = "PENDING";
    public static final String TASK_APPROVED = "APPROVED";
    public static final String TASK_REJECTED = "REJECTED";
    public static final String TASK_SKIPPED  = "SKIPPED";

    public static final String TYPE_USER    = "USER";
    public static final String TYPE_ROLE    = "ROLE";
    public static final String TYPE_MANAGER = "MANAGER";
    public static final String TYPE_ADMIN   = "ADMIN";

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowStepRepository stepRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowTaskRepository taskRepository;
    private final JdbcTemplate jdbcTemplate;

    // ============================================================
    // Definitions
    // ============================================================

    @Override
    public List<WorkflowDefinitionDto> listDefinitions(String moduleCode) {
        List<WorkflowDefinition> defs = (moduleCode == null || moduleCode.isBlank())
                ? definitionRepository.findByIsDeletedFalseOrderByModuleCodeAscCreatedDateDesc()
                : definitionRepository.findByModuleCodeAndActiveTrueAndIsDeletedFalseOrderByCreatedDateDesc(moduleCode);
        // Gallery templates (isTemplate) không lẫn vào SIMPLE list /wf
        return defs.stream()
                .filter(d -> !Boolean.TRUE.equals(d.getIsTemplate()))
                .map(this::mapDefinitionWithSteps)
                .toList();
    }

    @Override
    public WorkflowDefinitionDto getDefinition(String id) {
        WorkflowDefinition def = definitionRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new AppException(WorkflowErrorCode.DEFINITION_NOT_FOUND));
        return mapDefinitionWithSteps(def);
    }

    @Override
    public WorkflowDefinitionDto getDefinitionByCode(String code) {
        WorkflowDefinition def = definitionRepository.findByCode(code)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new AppException(WorkflowErrorCode.DEFINITION_NOT_FOUND));
        return mapDefinitionWithSteps(def);
    }

    @Override
    @Transactional
    public WorkflowDefinitionDto saveDefinition(WorkflowDefinitionDto dto) {
        if (dto.getName() == null || dto.getName().isBlank())
            throw new AppException(WorkflowErrorCode.DEFINITION_NAME_REQUIRED);
        if (dto.getCode() == null || dto.getCode().isBlank())
            throw new AppException(WorkflowErrorCode.DEFINITION_CODE_REQUIRED);
        if (dto.getModuleCode() == null || dto.getModuleCode().isBlank())
            throw new AppException(WorkflowErrorCode.DEFINITION_MODULE_REQUIRED);

        WorkflowDefinition def;
        boolean isNew = dto.getId() == null || dto.getId().isBlank();
        if (isNew) {
            if (definitionRepository.existsByCode(dto.getCode()))
                throw new AppException(WorkflowErrorCode.DEFINITION_CODE_DUPLICATE);
            def = WorkflowDefinition.builder()
                    .code(dto.getCode().trim().toUpperCase())
                    .name(dto.getName())
                    .moduleCode(dto.getModuleCode().trim().toUpperCase())
                    .description(dto.getDescription())
                    .active(dto.getActive() == null ? true : dto.getActive())
                    .editorMode(dto.getEditorMode() != null ? dto.getEditorMode() : "SIMPLE")
                    .version(dto.getVersion() != null ? dto.getVersion() : 1)
                    .isTemplate(Boolean.TRUE.equals(dto.getIsTemplate()))
                    .templateKey(dto.getTemplateKey())
                    .sourceTemplateCode(dto.getSourceTemplateCode())
                    .guideMarkdown(dto.getGuideMarkdown())
                    .build();
        } else {
            def = definitionRepository.findById(dto.getId())
                    .orElseThrow(() -> new AppException(WorkflowErrorCode.DEFINITION_NOT_FOUND));
            def.setName(dto.getName());
            def.setDescription(dto.getDescription());
            if (dto.getActive() != null) def.setActive(dto.getActive());
            // Không cho đổi code / moduleCode sau khi tạo
            // SIMPLE path không ghi đè graphJson — visual dùng /workflows/*
        }
        WorkflowDefinition saved = definitionRepository.save(def);

        // Upsert steps: delete all + reinsert (simple, không phải hot path)
        stepRepository.deleteByDefinitionId(saved.getId());
        List<WorkflowStepDto> stepDtos = dto.getSteps() != null ? dto.getSteps() : List.of();
        int order = 0;
        for (WorkflowStepDto s : stepDtos) {
            String type = s.getApproverType() != null ? s.getApproverType().toUpperCase() : TYPE_ADMIN;
            validateApproverType(type);
            stepRepository.save(WorkflowStep.builder()
                    .definitionId(saved.getId())
                    .stepOrder(order++)
                    .stepName(s.getStepName() != null ? s.getStepName() : "Bước " + order)
                    .approverType(type)
                    .approverValue(s.getApproverValue())
                    .allowSkip(Boolean.TRUE.equals(s.getAllowSkip()))
                    .slaHours(s.getSlaHours())
                    .description(s.getDescription())
                    .build());
        }
        log.info("[wf] {} definition {} ({}) với {} bước",
                isNew ? "Tạo" : "Cập nhật", saved.getCode(), saved.getId(), order);
        return mapDefinitionWithSteps(saved);
    }

    @Override
    @Transactional
    public void deleteDefinition(String id) {
        WorkflowDefinition def = definitionRepository.findById(id)
                .orElseThrow(() -> new AppException(WorkflowErrorCode.DEFINITION_NOT_FOUND));
        def.softDelete(SystemUtils.getCurrentUsername());
        definitionRepository.save(def);
    }

    // ============================================================
    // Instances
    // ============================================================

    @Override
    @Transactional
    public WorkflowInstanceDto start(String definitionCode, String entityType, String entityId,
                                     String startedBy, String title) {
        WorkflowDefinition def = definitionRepository.findByCode(definitionCode)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new AppException(WorkflowErrorCode.DEFINITION_NOT_FOUND));
        if (!Boolean.TRUE.equals(def.getActive())) {
            throw new AppException(WorkflowErrorCode.DEFINITION_INACTIVE);
        }

        List<WorkflowStep> steps = stepRepository.findByDefinitionIdAndIsDeletedFalseOrderByStepOrderAsc(def.getId());
        if (steps.isEmpty()) {
            throw new AppException(WorkflowErrorCode.DEFINITION_NO_STEPS);
        }

        WorkflowInstance inst = WorkflowInstance.builder()
                .definitionCode(def.getCode())
                .entityType(entityType)
                .entityId(entityId)
                .startedBy(startedBy != null ? startedBy : SystemUtils.getCurrentUsername())
                .startedAt(LocalDateTime.now())
                .currentStep(0)
                .status(STATUS_RUNNING)
                .title(title)
                .build();
        instanceRepository.save(inst);

        createTaskForStep(inst, steps.get(0));
        log.info("[wf] Started instance {} for {}#{} (def={})", inst.getId(), entityType, entityId, def.getCode());
        return toInstanceDto(inst, def, steps);
    }

    @Override
    public Optional<WorkflowInstanceDto> findInstanceByEntity(String entityType, String entityId) {
        return instanceRepository.findFirstByEntityTypeAndEntityIdOrderByStartedAtDesc(entityType, entityId)
                .map(inst -> {
                    WorkflowDefinition def = definitionRepository.findByCode(inst.getDefinitionCode()).orElse(null);
                    List<WorkflowStep> steps = def != null
                            ? stepRepository.findByDefinitionIdAndIsDeletedFalseOrderByStepOrderAsc(def.getId())
                            : List.of();
                    return toInstanceDto(inst, def, steps);
                });
    }

    @Override
    @Transactional
    public WorkflowInstanceDto cancelInstance(String instanceId) {
        WorkflowInstance inst = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new AppException(WorkflowErrorCode.INSTANCE_NOT_FOUND));
        if (!STATUS_RUNNING.equals(inst.getStatus())) {
            throw new AppException(WorkflowErrorCode.INSTANCE_NOT_RUNNING);
        }
        String me = SystemUtils.getCurrentUsername();
        if (!inst.getStartedBy().equalsIgnoreCase(me) && !isCurrentUserAdmin()) {
            throw new AppException(WorkflowErrorCode.INSTANCE_CANCEL_FORBIDDEN);
        }
        inst.setStatus(STATUS_CANCELLED);
        inst.setCompletedAt(LocalDateTime.now());
        instanceRepository.save(inst);

        // Skip pending tasks
        taskRepository.findByInstanceIdOrderByStepOrderAsc(instanceId).stream()
                .filter(t -> TASK_PENDING.equals(t.getStatus()))
                .forEach(t -> {
                    t.setStatus(TASK_SKIPPED);
                    t.setDecidedAt(LocalDateTime.now());
                    t.setDecidedBy(me);
                    taskRepository.save(t);
                });

        WorkflowDefinition def = definitionRepository.findByCode(inst.getDefinitionCode()).orElse(null);
        List<WorkflowStep> steps = def != null
                ? stepRepository.findByDefinitionIdAndIsDeletedFalseOrderByStepOrderAsc(def.getId())
                : List.of();
        return toInstanceDto(inst, def, steps);
    }

    // ============================================================
    // Tasks — inbox + actions
    // ============================================================

    @Override
    public List<WorkflowTaskDto> myPendingTasks() {
        String me = SystemUtils.getCurrentUsername();
        if (me == null) return List.of();
        List<String> roles = getRolesOfUser(me);
        boolean admin = isCurrentUserAdmin();
        return taskRepository.findPendingForUser(me, roles.isEmpty() ? List.of("__none__") : roles, admin)
                .stream()
                .map(this::mapTaskWithInstance)
                .toList();
    }

    @Override
    @Transactional
    public WorkflowTaskDto approveTask(String taskId, String comment) {
        WorkflowTask task = findTaskOrThrow(taskId);
        assertCanDecide(task);
        if (!TASK_PENDING.equals(task.getStatus())) {
            throw new AppException(WorkflowErrorCode.TASK_NOT_PENDING);
        }

        String me = SystemUtils.getCurrentUsername();
        task.setStatus(TASK_APPROVED);
        task.setDecidedBy(me);
        task.setDecidedAt(LocalDateTime.now());
        task.setComment(comment);
        taskRepository.save(task);

        // Advance instance
        WorkflowInstance inst = instanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new AppException(WorkflowErrorCode.INSTANCE_NOT_FOUND));
        WorkflowDefinition def = definitionRepository.findByCode(inst.getDefinitionCode()).orElse(null);
        List<WorkflowStep> steps = def != null
                ? stepRepository.findByDefinitionIdAndIsDeletedFalseOrderByStepOrderAsc(def.getId())
                : List.of();

        int nextIdx = task.getStepOrder() + 1;
        if (nextIdx >= steps.size()) {
            inst.setStatus(STATUS_COMPLETED);
            inst.setCompletedAt(LocalDateTime.now());
            instanceRepository.save(inst);
            log.info("[wf] Instance {} COMPLETED", inst.getId());
        } else {
            inst.setCurrentStep(nextIdx);
            instanceRepository.save(inst);
            createTaskForStep(inst, steps.get(nextIdx));
        }
        return mapTaskWithInstance(task);
    }

    @Override
    @Transactional
    public WorkflowTaskDto rejectTask(String taskId, String reason) {
        WorkflowTask task = findTaskOrThrow(taskId);
        assertCanDecide(task);
        if (!TASK_PENDING.equals(task.getStatus())) {
            throw new AppException(WorkflowErrorCode.TASK_NOT_PENDING);
        }
        if (reason == null || reason.isBlank()) {
            throw new AppException(WorkflowErrorCode.TASK_REJECT_REASON_REQUIRED);
        }

        String me = SystemUtils.getCurrentUsername();
        task.setStatus(TASK_REJECTED);
        task.setDecidedBy(me);
        task.setDecidedAt(LocalDateTime.now());
        task.setComment(reason);
        taskRepository.save(task);

        // Terminate instance
        WorkflowInstance inst = instanceRepository.findById(task.getInstanceId())
                .orElseThrow(() -> new AppException(WorkflowErrorCode.INSTANCE_NOT_FOUND));
        inst.setStatus(STATUS_REJECTED);
        inst.setCompletedAt(LocalDateTime.now());
        instanceRepository.save(inst);
        return mapTaskWithInstance(task);
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private void createTaskForStep(WorkflowInstance inst, WorkflowStep step) {
        String assignee = null;
        String role = null;
        String type = step.getApproverType() != null ? step.getApproverType().toUpperCase() : TYPE_ADMIN;
        switch (type) {
            case TYPE_USER -> assignee = step.getApproverValue();
            case TYPE_ROLE -> role = step.getApproverValue();
            case TYPE_MANAGER, TYPE_ADMIN -> {
                // Leave both null → pool inbox (admin) — MANAGER cần resolver runtime,
                // tạm fallback về ADMIN pool để không block flow.
            }
            default -> {
                // Should not reach — validateApproverType đã check khi save definition
            }
        }
        WorkflowTask task = WorkflowTask.builder()
                .instanceId(inst.getId())
                .stepOrder(step.getStepOrder())
                .stepName(step.getStepName())
                .approverType(type)
                .assigneeUsername(assignee)
                .assigneeRole(role)
                .status(TASK_PENDING)
                .deadline(step.getSlaHours() != null
                        ? LocalDateTime.now().plusHours(step.getSlaHours()) : null)
                .build();
        taskRepository.save(task);
    }

    private WorkflowTask findTaskOrThrow(String id) {
        return taskRepository.findById(id)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new AppException(WorkflowErrorCode.TASK_NOT_FOUND));
    }

    /**
     * Kiểm tra current user có quyền quyết định task không:
     * <ul>
     *   <li>USER task → assigneeUsername phải khớp</li>
     *   <li>ROLE task → user phải có role tương ứng</li>
     *   <li>ADMIN / MANAGER task → user phải là admin</li>
     * </ul>
     */
    private void assertCanDecide(WorkflowTask task) {
        String me = SystemUtils.getCurrentUsername();
        if (me == null) throw new AppException(WorkflowErrorCode.TASK_FORBIDDEN);
        if (isCurrentUserAdmin()) return; // Admin over-ride
        if (task.getAssigneeUsername() != null) {
            if (!task.getAssigneeUsername().equalsIgnoreCase(me))
                throw new AppException(WorkflowErrorCode.TASK_FORBIDDEN);
            return;
        }
        if (task.getAssigneeRole() != null) {
            List<String> roles = getRolesOfUser(me);
            if (roles.stream().noneMatch(r -> r.equalsIgnoreCase(task.getAssigneeRole())))
                throw new AppException(WorkflowErrorCode.TASK_FORBIDDEN);
            return;
        }
        // Pool cho admin only nhưng user không phải admin
        throw new AppException(WorkflowErrorCode.TASK_FORBIDDEN);
    }

    private static void validateApproverType(String type) {
        if (!(TYPE_USER.equals(type) || TYPE_ROLE.equals(type)
                || TYPE_MANAGER.equals(type) || TYPE_ADMIN.equals(type))) {
            throw new AppException(WorkflowErrorCode.STEP_APPROVER_TYPE_INVALID);
        }
    }

    /**
     * Query roles của 1 user qua JDBC (avoid cross-module dep — module-common là upstream).
     */
    private List<String> getRolesOfUser(String username) {
        if (username == null || username.isBlank()) return List.of();
        try {
            return jdbcTemplate.queryForList(
                    "SELECT DISTINCT r.code FROM users u " +
                            "JOIN user_role ur ON ur.user_id = u.id " +
                            "JOIN roles r ON r.id = ur.role_id " +
                            "WHERE u.user_name = ? " +
                            "AND (u.is_deleted IS NULL OR u.is_deleted = false) " +
                            "AND (ur.is_deleted IS NULL OR ur.is_deleted = false) " +
                            "AND (r.is_deleted IS NULL OR r.is_deleted = false)",
                    String.class, username);
        } catch (Exception ex) {
            log.warn("[wf] Không query được roles cho '{}': {}", username, ex.getMessage());
            return List.of();
        }
    }

    private boolean isCurrentUserAdmin() {
        String username = SystemUtils.getCurrentUsername();
        if (username == null) return false;
        String lower = username.toLowerCase();
        if ("admin".equals(lower) || "superadmin".equals(lower)) return true;
        try {
            Integer cnt = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users u JOIN person p ON p.id = u.person_id " +
                            "WHERE u.user_name = ? AND p.is_admin = true",
                    Integer.class, username);
            return cnt != null && cnt > 0;
        } catch (EmptyResultDataAccessException e) {
            return false;
        } catch (Exception ex) {
            log.warn("[wf] isAdmin check failed for '{}': {}", username, ex.getMessage());
            return false;
        }
    }

    // ============================================================
    // Mappers
    // ============================================================

    private WorkflowDefinitionDto mapDefinitionWithSteps(WorkflowDefinition def) {
        WorkflowDefinitionDto d = new WorkflowDefinitionDto();
        d.setId(def.getId());
        d.setCode(def.getCode());
        d.setName(def.getName());
        d.setModuleCode(def.getModuleCode());
        d.setDescription(def.getDescription());
        d.setActive(def.getActive());
        d.setCreatedBy(def.getCreatedBy());
        d.setCreatedDate(def.getCreatedDate() != null ? def.getCreatedDate().toString() : null);
        d.setEditorMode(def.getEditorMode() != null ? def.getEditorMode() : "SIMPLE");
        d.setGuideMarkdown(def.getGuideMarkdown());
        d.setTemplateKey(def.getTemplateKey());
        d.setSourceTemplateCode(def.getSourceTemplateCode());
        d.setVersion(def.getVersion());
        d.setIsTemplate(def.getIsTemplate());
        // graphJson chỉ parse ở VisualWorkflowService — SIMPLE list không cần payload nặng
        d.setSteps(stepRepository.findByDefinitionIdAndIsDeletedFalseOrderByStepOrderAsc(def.getId())
                .stream().map(this::mapStep).toList());
        return d;
    }

    private WorkflowStepDto mapStep(WorkflowStep s) {
        WorkflowStepDto d = new WorkflowStepDto();
        d.setId(s.getId());
        d.setStepOrder(s.getStepOrder());
        d.setStepName(s.getStepName());
        d.setApproverType(s.getApproverType());
        d.setApproverValue(s.getApproverValue());
        d.setAllowSkip(s.getAllowSkip());
        d.setSlaHours(s.getSlaHours());
        d.setDescription(s.getDescription());
        return d;
    }

    private WorkflowInstanceDto toInstanceDto(WorkflowInstance inst, WorkflowDefinition def, List<WorkflowStep> steps) {
        WorkflowInstanceDto dto = new WorkflowInstanceDto();
        dto.setId(inst.getId());
        dto.setDefinitionCode(inst.getDefinitionCode());
        dto.setDefinitionName(def != null ? def.getName() : null);
        dto.setEntityType(inst.getEntityType());
        dto.setEntityId(inst.getEntityId());
        dto.setTitle(inst.getTitle());
        dto.setStartedBy(inst.getStartedBy());
        dto.setStartedAt(inst.getStartedAt() != null ? inst.getStartedAt().toString() : null);
        dto.setCurrentStep(inst.getCurrentStep());
        dto.setStatus(inst.getStatus());
        dto.setCompletedAt(inst.getCompletedAt() != null ? inst.getCompletedAt().toString() : null);
        dto.setSteps(steps.stream().map(this::mapStep).toList());
        dto.setTasks(taskRepository.findByInstanceIdOrderByStepOrderAsc(inst.getId())
                .stream().map(t -> {
                    WorkflowTaskDto td = mapTaskLite(t);
                    // Không cần enrich lại instance vì đã có sẵn context
                    return td;
                }).toList());
        return dto;
    }

    private WorkflowTaskDto mapTaskLite(WorkflowTask t) {
        WorkflowTaskDto td = new WorkflowTaskDto();
        td.setId(t.getId());
        td.setInstanceId(t.getInstanceId());
        td.setStepOrder(t.getStepOrder());
        td.setStepName(t.getStepName());
        td.setApproverType(t.getApproverType());
        td.setAssigneeUsername(t.getAssigneeUsername());
        td.setAssigneeRole(t.getAssigneeRole());
        td.setStatus(t.getStatus());
        td.setDecidedBy(t.getDecidedBy());
        td.setDecidedAt(t.getDecidedAt() != null ? t.getDecidedAt().toString() : null);
        td.setComment(t.getComment());
        td.setDeadline(t.getDeadline() != null ? t.getDeadline().toString() : null);
        td.setCreatedDate(t.getCreatedDate() != null ? t.getCreatedDate().toString() : null);
        return td;
    }

    private WorkflowTaskDto mapTaskWithInstance(WorkflowTask t) {
        WorkflowTaskDto td = mapTaskLite(t);
        instanceRepository.findById(t.getInstanceId()).ifPresent(inst -> {
            td.setEntityType(inst.getEntityType());
            td.setEntityId(inst.getEntityId());
            td.setInstanceTitle(inst.getTitle());
            td.setInstanceStartedBy(inst.getStartedBy());
        });
        return td;
    }
}
