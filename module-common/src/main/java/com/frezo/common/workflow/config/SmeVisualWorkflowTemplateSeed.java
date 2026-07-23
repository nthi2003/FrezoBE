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
 * Seed bộ mẫu visual workflow chuẩn SME VN — gallery {@code isTemplate=true}.
 * Mỗi graph có DECISION với ≥ 2 cạnh ra (Có/Không hoặc Duyệt/Từ chối). Idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SmeVisualWorkflowTemplateSeed {

    private final WorkflowDefinitionRepository definitionRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        seed("LEAVE_APPROVAL_VISUAL", "Duyệt đơn nghỉ phép", "LEAVE",
                "Nhân viên xin nghỉ → Quản lý → HR → Kết thúc / Từ chối",
                buildLeaveApproval(), GUIDE_LEAVE);
        seed("PR_TO_PO_APPROVAL", "Duyệt mua hàng PR → PO", "PURCHASE",
                "Tạo PR → Duyệt ngân sách → Tạo PO → Duyệt PO → Kết thúc",
                buildPrToPo(), GUIDE_PR_PO);
        seed("STOCK_ISSUE_APPROVAL", "Duyệt xuất kho PXK", "INVENTORY",
                "Tạo phiếu xuất kho → Thủ kho kiểm → Duyệt xuất → Xuất kho / Từ chối",
                buildStockIssue(), GUIDE_PXK);
        seed("EXPENSE_ADVANCE_APPROVAL", "Duyệt tạm ứng / chi phí", "FINANCE",
                "Đề nghị tạm ứng → Quản lý → Kế toán → Chi tiền / Từ chối",
                buildExpenseAdvance(), GUIDE_EXPENSE);
        seed("HR_ONBOARDING", "Onboarding nhân sự", "HR",
                "Tiếp nhận NV mới → IT cấp tài khoản → Đào tạo → Hoàn tất / Dừng",
                buildOnboarding(), GUIDE_ONBOARD);
        seed("PROBATION_EVAL_PASS_FAIL", "Đánh giá thử việc Pass/Fail", "HR",
                "Đánh giá thử việc → Quản lý chốt Pass/Fail → Chính thức / Kết thúc HĐ",
                buildProbationEval(), GUIDE_PROBATION);
        seed("CUSTOMER_COMPLAINT", "Khiếu nại khách hàng", "CRM",
                "Tiếp nhận khiếu nại → Phân loại → Xử lý → Đóng / Leo thang",
                buildCustomerComplaint(), GUIDE_COMPLAINT);
        seed("STOCK_TRANSFER_APPROVAL", "Duyệt điều chuyển kho", "INVENTORY",
                "Yêu cầu điều chuyển → Kho nguồn → Kho đích → Hoàn tất / Từ chối",
                buildStockTransfer(), GUIDE_TRANSFER);
        seed("IT_ACCOUNT_LOCK_RESET", "Reset / khóa tài khoản IT", "IT",
                "Yêu cầu IT → Xác minh → Duyệt → Reset hoặc khóa / Từ chối",
                buildItAccount(), GUIDE_IT);
        seed("INVOICE_APPROVAL", "Phê duyệt hóa đơn", "FINANCE",
                "Nhận hóa đơn → Đối chiếu PO → Duyệt thanh toán → Chi / Từ chối",
                buildInvoiceApproval(), GUIDE_INVOICE);
        seed("ASSET_HANDOVER_VISUAL", "Cấp phát / bàn giao tài sản", "ASSET",
                "Yêu cầu tài sản → Admin duyệt → Bàn giao → Hoàn tất / Từ chối",
                buildAssetHandover(), GUIDE_ASSET);
        seed("CONTRACT_SIGN_VISUAL", "Phê duyệt ký hợp đồng", "CONTRACT",
                "Soạn HĐ → Legal → Tài chính → CEO ký / Từ chối",
                buildContractSign(), GUIDE_CONTRACT);
    }

    private void seed(String code, String name, String module, String description,
                      WorkflowGraphDto graph, String guide) {
        try {
            if (definitionRepository.existsByCode(code)) {
                definitionRepository.findByCode(code).ifPresent(def -> backfillIfNeeded(def, code, graph, guide));
                return;
            }
            String json = objectMapper.writeValueAsString(graph);
            WorkflowDefinition def = WorkflowDefinition.builder()
                    .code(code)
                    .name(name)
                    .moduleCode(module)
                    .description(description)
                    .active(true)
                    .editorMode("VISUAL")
                    .graphJson(json)
                    .guideMarkdown(guide)
                    .templateKey(code)
                    .sourceTemplateCode(null)
                    .version(1)
                    .isTemplate(true)
                    .build();
            definitionRepository.save(def);
            log.info("[wf-seed] Đã seed template SME {}", code);
        } catch (Exception ex) {
            log.warn("[wf-seed] Bỏ qua seed {}: {}", code, ex.getMessage());
        }
    }

    private void backfillIfNeeded(WorkflowDefinition def, String code,
                                  WorkflowGraphDto graph, String guide) {
        try {
            boolean dirty = false;
            if (!Boolean.TRUE.equals(def.getIsTemplate())) {
                def.setIsTemplate(true);
                dirty = true;
            }
            if (def.getTemplateKey() == null || def.getTemplateKey().isBlank()) {
                def.setTemplateKey(code);
                dirty = true;
            }
            if (def.getEditorMode() == null || def.getEditorMode().isBlank()) {
                def.setEditorMode("VISUAL");
                dirty = true;
            }
            if (def.getGraphJson() == null || def.getGraphJson().isBlank()) {
                def.setGraphJson(objectMapper.writeValueAsString(graph));
                dirty = true;
            }
            if (def.getGuideMarkdown() == null || def.getGuideMarkdown().isBlank()) {
                def.setGuideMarkdown(guide);
                dirty = true;
            }
            if (def.getVersion() == null) {
                def.setVersion(1);
                dirty = true;
            }
            if (dirty) {
                definitionRepository.save(def);
                log.info("[wf-seed] Backfill visual fields cho {}", code);
            }
        } catch (Exception ex) {
            log.warn("[wf-seed] Backfill {} thất bại: {}", code, ex.getMessage());
        }
    }

    // ---- Graphs (mỗi DECISION ≥ 2 cạnh ra) ----

    /** 1. Nghỉ phép: NV → QL → HR */
    static WorkflowGraphDto buildLeaveApproval() {
        String nv = "lane-nv", ql = "lane-ql", hr = "lane-hr";
        WorkflowGraphDto g = base(List.of(
                lane(nv, "Nhân viên", 0),
                lane(ql, "Quản lý", 1),
                lane(hr, "HR", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", nv, 40, 40),
                node("n-submit", "ACTION", "Tạo đơn nghỉ phép", nv, 220, 40),
                node("n-mgr", "APPROVAL", "Quản lý duyệt", ql, 440, 200),
                node("n-dec-mgr", "DECISION", "Quản lý đồng ý?", ql, 660, 200),
                node("n-hr", "APPROVAL", "HR xác nhận quỹ phép", hr, 880, 360),
                node("n-dec-hr", "DECISION", "HR duyệt?", hr, 1100, 360),
                node("n-end-ok", "END", "Đã duyệt nghỉ", hr, 1320, 360),
                node("n-end-no", "END", "Từ chối đơn", nv, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-submit", null),
                edge("e2", "n-submit", "n-mgr", null),
                edge("e3", "n-mgr", "n-dec-mgr", null),
                edge("e4-yes", "n-dec-mgr", "n-hr", "Duyệt"),
                edge("e4-no", "n-dec-mgr", "n-end-no", "Từ chối"),
                edge("e5", "n-hr", "n-dec-hr", null),
                edge("e6-yes", "n-dec-hr", "n-end-ok", "Duyệt"),
                edge("e6-no", "n-dec-hr", "n-end-no", "Từ chối")
        ));
        return g;
    }

    /** 2. PR → PO */
    static WorkflowGraphDto buildPrToPo() {
        String nv = "lane-req", mgr = "lane-mgr", purch = "lane-purch";
        WorkflowGraphDto g = base(List.of(
                lane(nv, "Người yêu cầu", 0),
                lane(mgr, "Quản lý / Ngân sách", 1),
                lane(purch, "Mua hàng", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", nv, 40, 40),
                node("n-pr", "ACTION", "Tạo phiếu yêu cầu mua (PR)", nv, 220, 40),
                node("n-budget", "APPROVAL", "Duyệt ngân sách", mgr, 440, 200),
                node("n-dec-budget", "DECISION", "Đủ ngân sách?", mgr, 660, 200),
                node("n-po", "ACTION", "Tạo đơn mua hàng (PO)", purch, 880, 360),
                node("n-po-appr", "APPROVAL", "Duyệt PO", purch, 1100, 360),
                node("n-dec-po", "DECISION", "PO được duyệt?", purch, 1320, 360),
                node("n-end-ok", "END", "PO đã duyệt", purch, 1540, 360),
                node("n-end-no", "END", "Từ chối mua hàng", nv, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-pr", null),
                edge("e2", "n-pr", "n-budget", null),
                edge("e3", "n-budget", "n-dec-budget", null),
                edge("e4-yes", "n-dec-budget", "n-po", "Có"),
                edge("e4-no", "n-dec-budget", "n-end-no", "Không"),
                edge("e5", "n-po", "n-po-appr", null),
                edge("e6", "n-po-appr", "n-dec-po", null),
                edge("e7-yes", "n-dec-po", "n-end-ok", "Duyệt"),
                edge("e7-no", "n-dec-po", "n-end-no", "Từ chối")
        ));
        return g;
    }

    /** 3. Xuất kho PXK */
    static WorkflowGraphDto buildStockIssue() {
        String req = "lane-req", wh = "lane-wh", mgr = "lane-mgr";
        WorkflowGraphDto g = base(List.of(
                lane(req, "Người yêu cầu", 0),
                lane(wh, "Thủ kho", 1),
                lane(mgr, "Quản lý kho", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", req, 40, 40),
                node("n-pxk", "ACTION", "Tạo phiếu xuất kho (PXK)", req, 220, 40),
                node("n-check", "ACTION", "Thủ kho kiểm tồn", wh, 440, 200),
                node("n-dec-stock", "DECISION", "Đủ tồn kho?", wh, 660, 200),
                node("n-appr", "APPROVAL", "Quản lý duyệt xuất", mgr, 880, 360),
                node("n-dec-appr", "DECISION", "Cho phép xuất?", mgr, 1100, 360),
                node("n-issue", "ACTION", "Xuất kho & ghi sổ", wh, 1320, 200),
                node("n-end-ok", "END", "Đã xuất kho", wh, 1540, 200),
                node("n-end-no", "END", "Từ chối xuất", req, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-pxk", null),
                edge("e2", "n-pxk", "n-check", null),
                edge("e3", "n-check", "n-dec-stock", null),
                edge("e4-yes", "n-dec-stock", "n-appr", "Có"),
                edge("e4-no", "n-dec-stock", "n-end-no", "Không"),
                edge("e5", "n-appr", "n-dec-appr", null),
                edge("e6-yes", "n-dec-appr", "n-issue", "Duyệt"),
                edge("e6-no", "n-dec-appr", "n-end-no", "Từ chối"),
                edge("e7", "n-issue", "n-end-ok", null)
        ));
        return g;
    }

    /** 4. Tạm ứng / chi phí */
    static WorkflowGraphDto buildExpenseAdvance() {
        String nv = "lane-nv", mgr = "lane-mgr", acc = "lane-acc";
        WorkflowGraphDto g = base(List.of(
                lane(nv, "Nhân viên", 0),
                lane(mgr, "Quản lý", 1),
                lane(acc, "Kế toán", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", nv, 40, 40),
                node("n-req", "ACTION", "Tạo đề nghị tạm ứng / chi phí", nv, 220, 40),
                node("n-mgr", "APPROVAL", "Quản lý duyệt", mgr, 440, 200),
                node("n-dec-mgr", "DECISION", "Quản lý đồng ý?", mgr, 660, 200),
                node("n-acc", "APPROVAL", "Kế toán kiểm & chi", acc, 880, 360),
                node("n-dec-acc", "DECISION", "Chi tiền?", acc, 1100, 360),
                node("n-pay", "ACTION", "Chi tạm ứng / hoàn ứng", acc, 1320, 360),
                node("n-end-ok", "END", "Đã chi", acc, 1540, 360),
                node("n-end-no", "END", "Từ chối đề nghị", nv, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-req", null),
                edge("e2", "n-req", "n-mgr", null),
                edge("e3", "n-mgr", "n-dec-mgr", null),
                edge("e4-yes", "n-dec-mgr", "n-acc", "Duyệt"),
                edge("e4-no", "n-dec-mgr", "n-end-no", "Từ chối"),
                edge("e5", "n-acc", "n-dec-acc", null),
                edge("e6-yes", "n-dec-acc", "n-pay", "Chi"),
                edge("e6-no", "n-dec-acc", "n-end-no", "Từ chối"),
                edge("e7", "n-pay", "n-end-ok", null)
        ));
        return g;
    }

    /** 5. Onboarding */
    static WorkflowGraphDto buildOnboarding() {
        String hr = "lane-hr", it = "lane-it", mgr = "lane-mgr";
        WorkflowGraphDto g = base(List.of(
                lane(hr, "HR", 0),
                lane(it, "IT", 1),
                lane(mgr, "Quản lý trực tiếp", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", hr, 40, 40),
                node("n-recv", "ACTION", "Tiếp nhận nhân sự mới", hr, 220, 40),
                node("n-dec-start", "DECISION", "Đủ hồ sơ onboard?", hr, 440, 40),
                node("n-it", "ACTION", "Cấp tài khoản / thiết bị", it, 660, 200),
                node("n-train", "ACTION", "Đào tạo nội bộ", mgr, 880, 360),
                node("n-dec-ready", "DECISION", "Sẵn sàng làm việc?", mgr, 1100, 360),
                node("n-end-ok", "END", "Onboard hoàn tất", hr, 1320, 40),
                node("n-end-no", "END", "Dừng / bổ sung hồ sơ", hr, 660, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-recv", null),
                edge("e2", "n-recv", "n-dec-start", null),
                edge("e3-yes", "n-dec-start", "n-it", "Có"),
                edge("e3-no", "n-dec-start", "n-end-no", "Không"),
                edge("e4", "n-it", "n-train", null),
                edge("e5", "n-train", "n-dec-ready", null),
                edge("e6-yes", "n-dec-ready", "n-end-ok", "Đạt"),
                edge("e6-no", "n-dec-ready", "n-end-no", "Chưa đạt")
        ));
        return g;
    }

    /** 6. Thử việc Pass/Fail */
    static WorkflowGraphDto buildProbationEval() {
        String hr = "lane-hr", mgr = "lane-mgr";
        WorkflowGraphDto g = base(List.of(
                lane(hr, "HR", 0),
                lane(mgr, "Quản lý", 1)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", hr, 40, 40),
                node("n-eval", "ACTION", "Mở phiếu đánh giá thử việc", hr, 220, 40),
                node("n-mgr", "APPROVAL", "Quản lý đánh giá", mgr, 440, 200),
                node("n-dec", "DECISION", "Đạt thử việc?", mgr, 660, 200),
                node("n-pass", "ACTION", "Chuyển chính thức", hr, 880, 40),
                node("n-fail", "ACTION", "Kết thúc HĐ thử việc", hr, 880, 200),
                node("n-end-ok", "END", "Pass — chính thức", hr, 1100, 40),
                node("n-end-no", "END", "Fail — kết thúc", hr, 1100, 200)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-eval", null),
                edge("e2", "n-eval", "n-mgr", null),
                edge("e3", "n-mgr", "n-dec", null),
                edge("e4-yes", "n-dec", "n-pass", "Đạt"),
                edge("e4-no", "n-dec", "n-fail", "Không đạt"),
                edge("e5", "n-pass", "n-end-ok", null),
                edge("e6", "n-fail", "n-end-no", null)
        ));
        return g;
    }

    /** 7. Khiếu nại KH */
    static WorkflowGraphDto buildCustomerComplaint() {
        String cs = "lane-cs", ops = "lane-ops", mgr = "lane-mgr";
        WorkflowGraphDto g = base(List.of(
                lane(cs, "Chăm sóc KH", 0),
                lane(ops, "Vận hành", 1),
                lane(mgr, "Quản lý", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", cs, 40, 40),
                node("n-recv", "ACTION", "Tiếp nhận khiếu nại", cs, 220, 40),
                node("n-dec-type", "DECISION", "Nghiêm trọng?", cs, 440, 40),
                node("n-fix", "ACTION", "Xử lý khiếu nại", ops, 660, 200),
                node("n-esc", "APPROVAL", "Leo thang quản lý", mgr, 660, 360),
                node("n-dec-done", "DECISION", "KH hài lòng?", cs, 880, 40),
                node("n-end-ok", "END", "Đóng khiếu nại", cs, 1100, 40),
                node("n-end-no", "END", "Mở lại / theo dõi", cs, 1100, 200)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-recv", null),
                edge("e2", "n-recv", "n-dec-type", null),
                edge("e3-no", "n-dec-type", "n-fix", "Không"),
                edge("e3-yes", "n-dec-type", "n-esc", "Có"),
                edge("e4", "n-esc", "n-fix", null),
                edge("e5", "n-fix", "n-dec-done", null),
                edge("e6-yes", "n-dec-done", "n-end-ok", "Có"),
                edge("e6-no", "n-dec-done", "n-end-no", "Không")
        ));
        return g;
    }

    /** 8. Điều chuyển kho */
    static WorkflowGraphDto buildStockTransfer() {
        String req = "lane-req", src = "lane-src", dst = "lane-dst";
        WorkflowGraphDto g = base(List.of(
                lane(req, "Người yêu cầu", 0),
                lane(src, "Kho nguồn", 1),
                lane(dst, "Kho đích", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", req, 40, 40),
                node("n-req", "ACTION", "Tạo yêu cầu điều chuyển", req, 220, 40),
                node("n-src", "APPROVAL", "Kho nguồn xác nhận", src, 440, 200),
                node("n-dec-src", "DECISION", "Kho nguồn đồng ý?", src, 660, 200),
                node("n-dst", "APPROVAL", "Kho đích nhận hàng", dst, 880, 360),
                node("n-dec-dst", "DECISION", "Nhận đủ hàng?", dst, 1100, 360),
                node("n-end-ok", "END", "Điều chuyển xong", dst, 1320, 360),
                node("n-end-no", "END", "Từ chối / hủy", req, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-req", null),
                edge("e2", "n-req", "n-src", null),
                edge("e3", "n-src", "n-dec-src", null),
                edge("e4-yes", "n-dec-src", "n-dst", "Duyệt"),
                edge("e4-no", "n-dec-src", "n-end-no", "Từ chối"),
                edge("e5", "n-dst", "n-dec-dst", null),
                edge("e6-yes", "n-dec-dst", "n-end-ok", "Đủ"),
                edge("e6-no", "n-dec-dst", "n-end-no", "Thiếu / lỗi")
        ));
        return g;
    }

    /** 9. IT reset / khóa tài khoản */
    static WorkflowGraphDto buildItAccount() {
        String req = "lane-req", it = "lane-it", mgr = "lane-mgr";
        WorkflowGraphDto g = base(List.of(
                lane(req, "Người yêu cầu", 0),
                lane(mgr, "Quản lý", 1),
                lane(it, "IT", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", req, 40, 40),
                node("n-ticket", "ACTION", "Tạo yêu cầu IT (reset/khóa)", req, 220, 40),
                node("n-mgr", "APPROVAL", "Quản lý xác minh", mgr, 440, 200),
                node("n-dec-mgr", "DECISION", "Cho phép xử lý?", mgr, 660, 200),
                node("n-type", "DECISION", "Loại thao tác?", it, 880, 360),
                node("n-reset", "ACTION", "Reset mật khẩu", it, 1100, 280),
                node("n-lock", "ACTION", "Khóa tài khoản", it, 1100, 440),
                node("n-end-ok", "END", "Đã xử lý", it, 1320, 360),
                node("n-end-no", "END", "Từ chối yêu cầu", req, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-ticket", null),
                edge("e2", "n-ticket", "n-mgr", null),
                edge("e3", "n-mgr", "n-dec-mgr", null),
                edge("e4-yes", "n-dec-mgr", "n-type", "Duyệt"),
                edge("e4-no", "n-dec-mgr", "n-end-no", "Từ chối"),
                edge("e5-reset", "n-type", "n-reset", "Reset"),
                edge("e5-lock", "n-type", "n-lock", "Khóa"),
                edge("e6", "n-reset", "n-end-ok", null),
                edge("e7", "n-lock", "n-end-ok", null)
        ));
        return g;
    }

    /** 10. Phê duyệt hóa đơn */
    static WorkflowGraphDto buildInvoiceApproval() {
        String acc = "lane-acc", purch = "lane-purch", mgr = "lane-mgr";
        WorkflowGraphDto g = base(List.of(
                lane(acc, "Kế toán", 0),
                lane(purch, "Mua hàng", 1),
                lane(mgr, "Quản lý tài chính", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", acc, 40, 40),
                node("n-recv", "ACTION", "Nhận hóa đơn NCC", acc, 220, 40),
                node("n-match", "ACTION", "Đối chiếu PO / biên nhận", purch, 440, 200),
                node("n-dec-match", "DECISION", "Khớp chứng từ?", purch, 660, 200),
                node("n-appr", "APPROVAL", "Duyệt thanh toán", mgr, 880, 360),
                node("n-dec-pay", "DECISION", "Thanh toán?", mgr, 1100, 360),
                node("n-pay", "ACTION", "Chi thanh toán", acc, 1320, 40),
                node("n-end-ok", "END", "Đã thanh toán", acc, 1540, 40),
                node("n-end-no", "END", "Từ chối / trả NCC", acc, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-recv", null),
                edge("e2", "n-recv", "n-match", null),
                edge("e3", "n-match", "n-dec-match", null),
                edge("e4-yes", "n-dec-match", "n-appr", "Khớp"),
                edge("e4-no", "n-dec-match", "n-end-no", "Lệch"),
                edge("e5", "n-appr", "n-dec-pay", null),
                edge("e6-yes", "n-dec-pay", "n-pay", "Duyệt"),
                edge("e6-no", "n-dec-pay", "n-end-no", "Từ chối"),
                edge("e7", "n-pay", "n-end-ok", null)
        ));
        return g;
    }

    /** 11. Cấp phát tài sản */
    static WorkflowGraphDto buildAssetHandover() {
        String nv = "lane-nv", adm = "lane-adm";
        WorkflowGraphDto g = base(List.of(
                lane(nv, "Nhân viên", 0),
                lane(adm, "Admin / HR", 1)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", nv, 40, 40),
                node("n-req", "ACTION", "Tạo yêu cầu cấp phát", nv, 220, 40),
                node("n-appr", "APPROVAL", "Admin duyệt yêu cầu", adm, 440, 200),
                node("n-dec", "DECISION", "Duyệt cấp phát?", adm, 660, 200),
                node("n-hand", "ACTION", "Bàn giao tài sản", adm, 880, 200),
                node("n-end-ok", "END", "Đã bàn giao", adm, 1100, 200),
                node("n-end-no", "END", "Từ chối yêu cầu", nv, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-req", null),
                edge("e2", "n-req", "n-appr", null),
                edge("e3", "n-appr", "n-dec", null),
                edge("e4-yes", "n-dec", "n-hand", "Duyệt"),
                edge("e4-no", "n-dec", "n-end-no", "Từ chối"),
                edge("e5", "n-hand", "n-end-ok", null)
        ));
        return g;
    }

    /** 12. Ký hợp đồng */
    static WorkflowGraphDto buildContractSign() {
        String legal = "lane-legal", fin = "lane-fin", ceo = "lane-ceo";
        WorkflowGraphDto g = base(List.of(
                lane(legal, "Legal", 0),
                lane(fin, "Tài chính", 1),
                lane(ceo, "CEO", 2)
        ));
        g.setNodes(List.of(
                node("n-start", "START", "Bắt đầu", legal, 40, 40),
                node("n-draft", "ACTION", "Soạn hợp đồng", legal, 220, 40),
                node("n-legal", "APPROVAL", "Legal review", legal, 440, 40),
                node("n-dec-legal", "DECISION", "Legal đạt?", legal, 660, 40),
                node("n-fin", "APPROVAL", "Tài chính review", fin, 880, 200),
                node("n-dec-fin", "DECISION", "Tài chính đạt?", fin, 1100, 200),
                node("n-ceo", "APPROVAL", "CEO ký", ceo, 1320, 360),
                node("n-dec-ceo", "DECISION", "CEO duyệt?", ceo, 1540, 360),
                node("n-end-ok", "END", "Hợp đồng đã ký", ceo, 1760, 360),
                node("n-end-no", "END", "Từ chối / dừng", legal, 880, 40)
        ));
        g.setEdges(List.of(
                edge("e1", "n-start", "n-draft", null),
                edge("e2", "n-draft", "n-legal", null),
                edge("e3", "n-legal", "n-dec-legal", null),
                edge("e4-yes", "n-dec-legal", "n-fin", "Đạt"),
                edge("e4-no", "n-dec-legal", "n-end-no", "Không đạt"),
                edge("e5", "n-fin", "n-dec-fin", null),
                edge("e6-yes", "n-dec-fin", "n-ceo", "Đạt"),
                edge("e6-no", "n-dec-fin", "n-end-no", "Không đạt"),
                edge("e7", "n-ceo", "n-dec-ceo", null),
                edge("e8-yes", "n-dec-ceo", "n-end-ok", "Duyệt"),
                edge("e8-no", "n-dec-ceo", "n-end-no", "Từ chối")
        ));
        return g;
    }

    // ---- Helpers ----

    private static WorkflowGraphDto base(List<WorkflowSwimlaneDto> lanes) {
        WorkflowGraphDto g = new WorkflowGraphDto();
        g.setVersion(1);
        g.setLanes(lanes);
        return g;
    }

    private static WorkflowSwimlaneDto lane(String id, String label, int order) {
        WorkflowSwimlaneDto l = new WorkflowSwimlaneDto();
        l.setId(id);
        l.setLabel(label);
        l.setOrder(order);
        return l;
    }

    private static WorkflowGraphNodeDto node(String id, String type, String label,
                                            String laneId, double x, double y) {
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

    // ---- Guides ----

    private static final String GUIDE_COMMON = """

            ## Quy tắc graph

            - Clone mẫu trước khi sửa — không sửa trực tiếp template hệ thống.
            - Node **DECISION** phải có ≥ 2 cạnh ra (thường **Duyệt/Từ chối** hoặc **Có/Không**).
            - Đúng **1 START**, ≥ **1 END**; mọi node reachable từ START.
            """;

    private static final String GUIDE_LEAVE = """
            # Duyệt đơn nghỉ phép

            Nhân viên tạo đơn → Quản lý duyệt → HR xác nhận quỹ phép.
            """ + GUIDE_COMMON;

    private static final String GUIDE_PR_PO = """
            # Duyệt mua hàng PR → PO

            Tạo PR → Duyệt ngân sách → Tạo PO → Duyệt PO.
            """ + GUIDE_COMMON;

    private static final String GUIDE_PXK = """
            # Duyệt xuất kho PXK

            Tạo PXK → Kiểm tồn → Duyệt xuất → Ghi sổ xuất kho.
            """ + GUIDE_COMMON;

    private static final String GUIDE_EXPENSE = """
            # Duyệt tạm ứng / chi phí

            Đề nghị → Quản lý → Kế toán chi tiền.
            """ + GUIDE_COMMON;

    private static final String GUIDE_ONBOARD = """
            # Onboarding nhân sự

            Tiếp nhận → Cấp tài khoản IT → Đào tạo → Sẵn sàng làm việc.
            """ + GUIDE_COMMON;

    private static final String GUIDE_PROBATION = """
            # Đánh giá thử việc Pass/Fail

            Mở phiếu → Quản lý đánh giá → Pass (chính thức) / Fail (kết thúc HĐ).
            """ + GUIDE_COMMON;

    private static final String GUIDE_COMPLAINT = """
            # Khiếu nại khách hàng

            Tiếp nhận → Phân loại nghiêm trọng → Xử lý → Đóng hoặc theo dõi.
            """ + GUIDE_COMMON;

    private static final String GUIDE_TRANSFER = """
            # Duyệt điều chuyển kho

            Yêu cầu → Kho nguồn xác nhận → Kho đích nhận hàng.
            """ + GUIDE_COMMON;

    private static final String GUIDE_IT = """
            # Reset / khóa tài khoản IT

            Ticket → Quản lý xác minh → IT Reset hoặc Khóa.
            """ + GUIDE_COMMON;

    private static final String GUIDE_INVOICE = """
            # Phê duyệt hóa đơn

            Nhận HĐ → Đối chiếu PO → Duyệt thanh toán → Chi.
            """ + GUIDE_COMMON;

    private static final String GUIDE_ASSET = """
            # Cấp phát / bàn giao tài sản

            Yêu cầu → Admin duyệt → Bàn giao vật lý.
            """ + GUIDE_COMMON;

    private static final String GUIDE_CONTRACT = """
            # Phê duyệt ký hợp đồng

            Soạn HĐ → Legal → Tài chính → CEO ký.
            """ + GUIDE_COMMON;
}
