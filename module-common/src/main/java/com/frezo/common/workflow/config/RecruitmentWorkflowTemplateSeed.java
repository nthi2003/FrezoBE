package com.frezo.common.workflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.common.workflow.dto.WorkflowGraphDto;
import com.frezo.common.workflow.dto.WorkflowGraphDto.PositionDto;
import com.frezo.common.workflow.dto.WorkflowGraphDto.WorkflowGraphEdgeDto;
import com.frezo.common.workflow.dto.WorkflowGraphDto.WorkflowGraphNodeDto;
import com.frezo.common.workflow.dto.WorkflowGraphDto.WorkflowSwimlaneDto;
import com.frezo.common.workflow.entity.WorkflowDefinition;
import com.frezo.common.workflow.repository.WorkflowDefinitionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seed mẫu visual {@code RECRUITMENT_DEFAULT} — lưu đồ tuyển dụng 3 lane
 * Hiring Manager / HR / CEO + Yes/No. Idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecruitmentWorkflowTemplateSeed {

    public static final String CODE = "RECRUITMENT_DEFAULT";

    private static final String LANE_HM = "lane-hm";
    private static final String LANE_HR = "lane-hr";
    private static final String LANE_CEO = "lane-ceo";

    private final WorkflowDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        try {
            if (definitionRepository.existsByCode(CODE)) {
                // Backfill graph nếu bản cũ thiếu visual fields
                definitionRepository.findByCode(CODE).ifPresent(this::backfillIfNeeded);
                return;
            }
            WorkflowGraphDto graph = buildRecruitmentGraph();
            String json = objectMapper.writeValueAsString(graph);
            WorkflowDefinition def = WorkflowDefinition.builder()
                    .code(CODE)
                    .name("Quy trình tuyển dụng (mặc định)")
                    .moduleCode("RECRUITMENT")
                    .description("Lưu đồ tuyển dụng: Yêu cầu → Nguồn → Sàng lọc → PV → Offer → Onboard")
                    .active(true)
                    .editorMode("VISUAL")
                    .graphJson(json)
                    .guideMarkdown(GUIDE_MARKDOWN)
                    .templateKey(CODE)
                    .sourceTemplateCode(null)
                    .version(1)
                    .isTemplate(true)
                    .build();
            definitionRepository.save(def);
            log.info("[wf-seed] Đã seed template {}", CODE);
        } catch (Exception ex) {
            log.warn("[wf-seed] Bỏ qua seed {}: {}", CODE, ex.getMessage());
        }
    }

    private void backfillIfNeeded(WorkflowDefinition def) {
        try {
            boolean dirty = false;
            if (!Boolean.TRUE.equals(def.getIsTemplate())) {
                def.setIsTemplate(true);
                dirty = true;
            }
            if (def.getTemplateKey() == null || def.getTemplateKey().isBlank()) {
                def.setTemplateKey(CODE);
                dirty = true;
            }
            if (def.getEditorMode() == null || def.getEditorMode().isBlank()) {
                def.setEditorMode("VISUAL");
                dirty = true;
            }
            if (def.getGraphJson() == null || def.getGraphJson().isBlank()) {
                def.setGraphJson(objectMapper.writeValueAsString(buildRecruitmentGraph()));
                dirty = true;
            }
            if (def.getGuideMarkdown() == null || def.getGuideMarkdown().isBlank()) {
                def.setGuideMarkdown(GUIDE_MARKDOWN);
                dirty = true;
            }
            if (def.getVersion() == null) {
                def.setVersion(1);
                dirty = true;
            }
            if (dirty) {
                definitionRepository.save(def);
                log.info("[wf-seed] Backfill visual fields cho {}", CODE);
            }
        } catch (Exception ex) {
            log.warn("[wf-seed] Backfill {} thất bại: {}", CODE, ex.getMessage());
        }
    }

    static WorkflowGraphDto buildRecruitmentGraph() {
        WorkflowGraphDto g = new WorkflowGraphDto();
        g.setVersion(1);
        g.setLanes(List.of(
                lane(LANE_HM, "Hiring Manager", 0),
                lane(LANE_HR, "HR", 1),
                lane(LANE_CEO, "CEO", 2)
        ));

        // Y: HM=40, HR=220, CEO=400 — X theo stage
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", LANE_HM, 40, 40),
                node("n-req", "ACTION", "Tạo yêu cầu tuyển dụng", LANE_HM, 220, 40),
                node("n-dec-req", "DECISION", "Duyệt yêu cầu?", LANE_HR, 420, 220),
                node("n-source", "ACTION", "Tạo nguồn / đăng tin", LANE_HR, 640, 220),
                node("n-screen", "ACTION", "Sàng lọc hồ sơ", LANE_HR, 860, 220),
                node("n-dec-screen", "DECISION", "Đạt sàng lọc?", LANE_HR, 1080, 220),
                node("n-iv-hm", "APPROVAL", "PV chuyên môn", LANE_HM, 1300, 40),
                node("n-iv-hr", "APPROVAL", "PV HR", LANE_HR, 1300, 220),
                node("n-dec-iv", "DECISION", "Đạt phỏng vấn?", LANE_HM, 1520, 40),
                node("n-offer", "ACTION", "Soạn Offer", LANE_HR, 1740, 220),
                node("n-dec-ceo", "DECISION", "CEO duyệt Offer?", LANE_CEO, 1960, 400),
                node("n-onboard", "ACTION", "Onboard", LANE_HR, 2180, 220),
                node("n-end-ok", "END", "Hoàn tất tuyển dụng", LANE_HR, 2400, 220),
                node("n-end-reject", "END", "Từ chối / dừng", LANE_HR, 1080, 400)
        ));

        g.setEdges(List.of(
                edge("e1", "n-start", "n-req", null),
                edge("e2", "n-req", "n-dec-req", null),
                edge("e3-yes", "n-dec-req", "n-source", "Yes"),
                edge("e3-no", "n-dec-req", "n-end-reject", "No"),
                edge("e4", "n-source", "n-screen", null),
                edge("e5", "n-screen", "n-dec-screen", null),
                edge("e6-yes", "n-dec-screen", "n-iv-hm", "Yes"),
                edge("e6-no", "n-dec-screen", "n-end-reject", "No"),
                edge("e7", "n-iv-hm", "n-iv-hr", null),
                edge("e8", "n-iv-hr", "n-dec-iv", null),
                edge("e9-yes", "n-dec-iv", "n-offer", "Yes"),
                edge("e9-no", "n-dec-iv", "n-end-reject", "No"),
                edge("e10", "n-offer", "n-dec-ceo", null),
                edge("e11-yes", "n-dec-ceo", "n-onboard", "Yes"),
                edge("e11-no", "n-dec-ceo", "n-end-reject", "No"),
                edge("e12", "n-onboard", "n-end-ok", null)
        ));
        return g;
    }

    private static WorkflowSwimlaneDto lane(String id, String label, int order) {
        WorkflowSwimlaneDto l = new WorkflowSwimlaneDto();
        l.setId(id);
        l.setLabel(label);
        l.setOrder(order);
        return l;
    }

    private static WorkflowGraphNodeDto node(String id, String type, String label, String laneId, double x, double y) {
        WorkflowGraphNodeDto n = new WorkflowGraphNodeDto();
        n.setId(id);
        n.setType(type);
        n.setLabel(label);
        n.setLaneId(laneId);
        n.setPosition(new PositionDto(x, y));
        return n;
    }

    private static WorkflowGraphEdgeDto edge(String id, String source, String target, String label) {
        WorkflowGraphEdgeDto e = new WorkflowGraphEdgeDto();
        e.setId(id);
        e.setSource(source);
        e.setTarget(target);
        e.setLabel(label);
        return e;
    }

    static final String GUIDE_MARKDOWN = """
            # Quy trình tuyển dụng (mẫu Frezo)

            Mẫu này mô tả lưu đồ 3 swimlane: **Hiring Manager**, **HR**, **CEO**.

            ## Các giai đoạn

            1. **Yêu cầu** — Hiring Manager tạo yêu cầu tuyển dụng.
            2. **Duyệt yêu cầu** — HR xác nhận (Yes → tiếp tục / No → dừng).
            3. **Tạo nguồn** — HR đăng tin, thu hút ứng viên.
            4. **Sàng lọc** — HR lọc hồ sơ (Yes → PV / No → dừng).
            5. **Phỏng vấn** — PV chuyên môn (HM) rồi PV HR.
            6. **Quyết định PV** — HM chốt (Yes → Offer / No → dừng).
            7. **Offer** — HR soạn thư mời; CEO duyệt (Yes → Onboard / No → dừng).
            8. **Onboard** — HR hoàn tất tiếp nhận.

            ## Ghi chú chỉnh sửa

            - Clone mẫu trước khi sửa — không sửa trực tiếp template hệ thống.
            - Node **DECISION** phải có ít nhất 2 cạnh ra (thường gắn nhãn Yes / No).
            - Graph phải có đúng **1 START** và ít nhất **1 END**.
            - Mọi node phải reachable từ START (dùng nút Validate trên designer).
            """;
}
