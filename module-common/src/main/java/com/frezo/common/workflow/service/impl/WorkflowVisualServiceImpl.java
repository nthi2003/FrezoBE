package com.frezo.common.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.common.exception.AppException;
import com.frezo.common.workflow.WorkflowErrorCode;
import com.frezo.common.workflow.dto.WorkflowDefinitionDto;
import com.frezo.common.workflow.dto.WorkflowGraphDto;
import com.frezo.common.workflow.dto.WorkflowStepDto;
import com.frezo.common.workflow.dto.WorkflowTemplateMetaDto;
import com.frezo.common.workflow.dto.WorkflowValidateResultDto;
import com.frezo.common.workflow.entity.WorkflowDefinition;
import com.frezo.common.workflow.repository.WorkflowDefinitionRepository;
import com.frezo.common.workflow.repository.WorkflowStepRepository;
import com.frezo.common.workflow.service.WorkflowVisualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowVisualServiceImpl implements WorkflowVisualService {

    public static final String MODE_SIMPLE = "SIMPLE";
    public static final String MODE_VISUAL = "VISUAL";

    private final WorkflowDefinitionRepository definitionRepository;
    private final WorkflowStepRepository stepRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<WorkflowTemplateMetaDto> listTemplates() {
        return definitionRepository.findByIsTemplateTrueAndIsDeletedFalseOrderByModuleCodeAscNameAsc()
                .stream()
                .map(this::toTemplateMeta)
                .toList();
    }

    @Override
    public WorkflowDefinitionDto getTemplate(String code) {
        WorkflowDefinition def = findTemplate(code);
        return toDto(def);
    }

    @Override
    @Transactional
    public WorkflowDefinitionDto cloneTemplate(String code) {
        WorkflowDefinition src = findTemplate(code);
        String newCode = uniqueCloneCode(src.getCode());
        WorkflowDefinition clone = WorkflowDefinition.builder()
                .code(newCode)
                .name(src.getName() + " (bản sao)")
                .moduleCode(src.getModuleCode())
                .description(src.getDescription())
                .active(true)
                .editorMode(MODE_VISUAL)
                .graphJson(src.getGraphJson())
                .guideMarkdown(src.getGuideMarkdown())
                .templateKey(null)
                .sourceTemplateCode(src.getCode())
                .version(src.getVersion() != null ? src.getVersion() : 1)
                .isTemplate(false)
                .build();
        WorkflowDefinition saved = definitionRepository.save(clone);
        log.info("[wf-visual] Clone template {} → definition {} ({})", src.getCode(), saved.getCode(), saved.getId());
        return toDto(saved);
    }

    @Override
    public WorkflowDefinitionDto getDefinitionVisual(String id) {
        return toDto(requireDefinition(id));
    }

    @Override
    @Transactional
    public WorkflowDefinitionDto updateDefinitionVisual(String id, WorkflowDefinitionDto dto) {
        WorkflowDefinition def = requireDefinition(id);
        if (Boolean.TRUE.equals(def.getIsTemplate())) {
            throw new AppException(WorkflowErrorCode.TEMPLATE_READ_ONLY);
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            def.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            def.setDescription(dto.getDescription());
        }
        if (dto.getActive() != null) {
            def.setActive(dto.getActive());
        }
        if (dto.getGuideMarkdown() != null) {
            def.setGuideMarkdown(dto.getGuideMarkdown());
        }
        if (dto.getEditorMode() != null && !dto.getEditorMode().isBlank()) {
            def.setEditorMode(dto.getEditorMode().trim().toUpperCase());
        } else if (def.getEditorMode() == null) {
            def.setEditorMode(MODE_VISUAL);
        }
        if (dto.getGraphJson() != null) {
            def.setGraphJson(writeGraph(dto.getGraphJson()));
            int next = def.getVersion() == null ? 1 : def.getVersion() + 1;
            if (dto.getGraphJson().getVersion() != null) {
                def.setVersion(dto.getGraphJson().getVersion());
            } else {
                def.setVersion(next);
            }
        } else if (dto.getVersion() != null) {
            def.setVersion(dto.getVersion());
        }
        WorkflowDefinition saved = definitionRepository.save(def);
        return toDto(saved);
    }

    @Override
    public WorkflowGraphDto getGraph(String definitionId) {
        WorkflowDefinition def = requireDefinition(definitionId);
        WorkflowGraphDto graph = readGraph(def.getGraphJson());
        if (graph.getVersion() == null && def.getVersion() != null) {
            graph.setVersion(def.getVersion());
        }
        return graph;
    }

    @Override
    @Transactional
    public WorkflowGraphDto saveGraph(String definitionId, WorkflowGraphDto graph) {
        WorkflowDefinition def = requireDefinition(definitionId);
        if (Boolean.TRUE.equals(def.getIsTemplate())) {
            throw new AppException(WorkflowErrorCode.TEMPLATE_READ_ONLY);
        }
        if (graph == null) {
            throw new AppException(WorkflowErrorCode.GRAPH_REQUIRED);
        }
        int next = def.getVersion() == null ? 1 : def.getVersion() + 1;
        if (graph.getVersion() == null) {
            graph.setVersion(next);
        }
        def.setGraphJson(writeGraph(graph));
        def.setVersion(graph.getVersion());
        def.setEditorMode(MODE_VISUAL);
        definitionRepository.save(def);
        return graph;
    }

    @Override
    public WorkflowValidateResultDto validate(String definitionId) {
        WorkflowDefinition def = requireDefinition(definitionId);
        WorkflowGraphDto graph = readGraph(def.getGraphJson());
        return validateGraph(graph);
    }

    // ---- Validation rules ----

    public static WorkflowValidateResultDto validateGraph(WorkflowGraphDto graph) {
        List<String> errors = new ArrayList<>();
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            return WorkflowValidateResultDto.fail(List.of("Graph phải có ít nhất 1 node"));
        }
        List<WorkflowGraphDto.WorkflowGraphNodeDto> nodes = graph.getNodes();
        List<WorkflowGraphDto.WorkflowGraphEdgeDto> edges =
                graph.getEdges() != null ? graph.getEdges() : List.of();

        long startCount = nodes.stream().filter(n -> "START".equalsIgnoreCase(n.getType())).count();
        if (startCount != 1) {
            errors.add("Phải có đúng 1 node START (hiện có " + startCount + ")");
        }
        long endCount = nodes.stream().filter(n -> "END".equalsIgnoreCase(n.getType())).count();
        if (endCount < 1) {
            errors.add("Phải có ít nhất 1 node END");
        }

        Map<String, List<String>> outgoing = new HashMap<>();
        Set<String> nodeIds = new HashSet<>();
        for (WorkflowGraphDto.WorkflowGraphNodeDto n : nodes) {
            if (n.getId() != null) nodeIds.add(n.getId());
        }
        for (WorkflowGraphDto.WorkflowGraphEdgeDto e : edges) {
            if (e.getSource() == null || e.getTarget() == null) continue;
            if (!nodeIds.contains(e.getSource()) || !nodeIds.contains(e.getTarget())) {
                errors.add("Edge " + e.getId() + " trỏ tới node không tồn tại");
                continue;
            }
            outgoing.computeIfAbsent(e.getSource(), k -> new ArrayList<>()).add(e.getTarget());
        }

        for (WorkflowGraphDto.WorkflowGraphNodeDto n : nodes) {
            if ("DECISION".equalsIgnoreCase(n.getType())) {
                int out = outgoing.getOrDefault(n.getId(), List.of()).size();
                if (out < 2) {
                    errors.add("Node DECISION '" + n.getLabel() + "' phải có ≥ 2 cạnh ra (hiện " + out + ")");
                }
            }
        }

        Optional<WorkflowGraphDto.WorkflowGraphNodeDto> startOpt = nodes.stream()
                .filter(n -> "START".equalsIgnoreCase(n.getType()))
                .findFirst();
        if (startOpt.isPresent() && startOpt.get().getId() != null) {
            Set<String> reachable = new HashSet<>();
            Deque<String> q = new ArrayDeque<>();
            q.add(startOpt.get().getId());
            reachable.add(startOpt.get().getId());
            while (!q.isEmpty()) {
                String cur = q.poll();
                for (String next : outgoing.getOrDefault(cur, List.of())) {
                    if (reachable.add(next)) q.add(next);
                }
            }
            for (WorkflowGraphDto.WorkflowGraphNodeDto n : nodes) {
                if (n.getId() != null && !reachable.contains(n.getId())) {
                    errors.add("Node '" + n.getLabel() + "' (" + n.getId() + ") không reachable từ START");
                }
            }
        }

        return errors.isEmpty() ? WorkflowValidateResultDto.ok() : WorkflowValidateResultDto.fail(errors);
    }

    // ---- Helpers ----

    private WorkflowDefinition findTemplate(String code) {
        String key = code != null ? code.trim() : "";
        return definitionRepository.findByTemplateKeyAndIsTemplateTrueAndIsDeletedFalse(key)
                .or(() -> definitionRepository.findByCodeAndIsTemplateTrueAndIsDeletedFalse(key.toUpperCase()))
                .or(() -> definitionRepository.findByCode(key.toUpperCase())
                        .filter(d -> Boolean.TRUE.equals(d.getIsTemplate()) && !Boolean.TRUE.equals(d.getIsDeleted())))
                .orElseThrow(() -> new AppException(WorkflowErrorCode.TEMPLATE_NOT_FOUND));
    }

    private WorkflowDefinition requireDefinition(String id) {
        return definitionRepository.findById(id)
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .orElseThrow(() -> new AppException(WorkflowErrorCode.DEFINITION_NOT_FOUND));
    }

    private String uniqueCloneCode(String base) {
        String prefix = (base != null ? base : "WF") + "_COPY_";
        String candidate;
        int guard = 0;
        do {
            candidate = prefix + Integer.toHexString((int) (System.currentTimeMillis() & 0xfffff)).toUpperCase()
                    + (guard > 0 ? "_" + guard : "");
            guard++;
        } while (definitionRepository.existsByCode(candidate) && guard < 20);
        return candidate;
    }

    private WorkflowTemplateMetaDto toTemplateMeta(WorkflowDefinition def) {
        WorkflowTemplateMetaDto m = new WorkflowTemplateMetaDto();
        m.setKey(def.getTemplateKey() != null ? def.getTemplateKey() : def.getCode());
        m.setName(def.getName());
        m.setDescription(def.getDescription());
        m.setModuleCode(def.getModuleCode());
        m.setUpdatedAt(def.getUpdatedDate() != null
                ? def.getUpdatedDate().toString()
                : (def.getCreatedDate() != null ? def.getCreatedDate().toString() : null));
        m.setGuideMarkdown(def.getGuideMarkdown());
        m.setVersion(def.getVersion());
        WorkflowGraphDto g = readGraph(def.getGraphJson());
        m.setNodeCount(g.getNodes() != null ? g.getNodes().size() : 0);
        return m;
    }

    private WorkflowDefinitionDto toDto(WorkflowDefinition def) {
        WorkflowDefinitionDto d = new WorkflowDefinitionDto();
        d.setId(def.getId());
        d.setCode(def.getCode());
        d.setName(def.getName());
        d.setModuleCode(def.getModuleCode());
        d.setDescription(def.getDescription());
        d.setActive(def.getActive());
        d.setCreatedBy(def.getCreatedBy());
        d.setCreatedDate(def.getCreatedDate() != null ? def.getCreatedDate().toString() : null);
        d.setEditorMode(def.getEditorMode() != null ? def.getEditorMode() : MODE_SIMPLE);
        d.setGraphJson(readGraph(def.getGraphJson()));
        d.setGuideMarkdown(def.getGuideMarkdown());
        d.setTemplateKey(def.getTemplateKey());
        d.setSourceTemplateCode(def.getSourceTemplateCode());
        d.setVersion(def.getVersion());
        d.setIsTemplate(def.getIsTemplate());
        List<WorkflowStepDto> steps = stepRepository
                .findByDefinitionIdAndIsDeletedFalseOrderByStepOrderAsc(def.getId())
                .stream()
                .map(s -> {
                    WorkflowStepDto sd = new WorkflowStepDto();
                    sd.setId(s.getId());
                    sd.setStepOrder(s.getStepOrder());
                    sd.setStepName(s.getStepName());
                    sd.setApproverType(s.getApproverType());
                    sd.setApproverValue(s.getApproverValue());
                    sd.setAllowSkip(s.getAllowSkip());
                    sd.setSlaHours(s.getSlaHours());
                    sd.setDescription(s.getDescription());
                    return sd;
                })
                .toList();
        d.setSteps(steps);
        return d;
    }

    private WorkflowGraphDto readGraph(String json) {
        if (json == null || json.isBlank()) {
            WorkflowGraphDto empty = new WorkflowGraphDto();
            empty.setVersion(1);
            empty.setLanes(new ArrayList<>());
            empty.setNodes(new ArrayList<>());
            empty.setEdges(new ArrayList<>());
            return empty;
        }
        try {
            return objectMapper.readValue(json, WorkflowGraphDto.class);
        } catch (JsonProcessingException ex) {
            log.warn("[wf-visual] Không parse được graphJson: {}", ex.getMessage());
            throw new AppException(WorkflowErrorCode.GRAPH_INVALID);
        }
    }

    private String writeGraph(WorkflowGraphDto graph) {
        try {
            return objectMapper.writeValueAsString(graph);
        } catch (JsonProcessingException ex) {
            throw new AppException(WorkflowErrorCode.GRAPH_INVALID);
        }
    }
}
