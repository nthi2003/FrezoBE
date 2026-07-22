package com.frezo.common.workflow.config;

import com.frezo.common.workflow.dto.WorkflowDefinitionDto;
import com.frezo.common.workflow.dto.WorkflowStepDto;
import com.frezo.common.workflow.repository.WorkflowDefinitionRepository;
import com.frezo.common.workflow.service.WorkflowService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Seed 3 workflow definition mặc định để user có sẵn cái mà dùng ngay:
 * <ul>
 *   <li>{@code ASSET_TRANSFER_DEFAULT} — Cấp phát tài sản (Admin duyệt → Admin bàn giao)</li>
 *   <li>{@code LEAVE_DEFAULT} — Đăng ký nghỉ phép (Manager → HR)</li>
 *   <li>{@code CONTRACT_APPROVE} — Hợp đồng (Legal → Finance → CEO)</li>
 * </ul>
 * <p>
 * <b>Idempotent</b>: skip nếu code đã tồn tại. Admin có thể vào editor UI để chỉnh sửa
 * mà không lo bị ghi đè khi restart.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowDataInitializer {

    private final WorkflowService workflowService;
    private final WorkflowDefinitionRepository definitionRepository;

    @PostConstruct
    public void init() {
        try {
            seedIfMissing("ASSET_TRANSFER_DEFAULT", "Cấp phát tài sản (mặc định)", "ASSET",
                    "Ticket cấp phát / thu hồi tài sản qua approval flow 2 bước.",
                    List.of(
                            step("Duyệt yêu cầu", "ADMIN", null, "Admin/HR review yêu cầu, xác nhận nhân viên nhận thực tế nhu cầu"),
                            step("Bàn giao", "ADMIN", null, "Xác nhận đã bàn giao vật lý — asset chuyển IN_USE")
                    ));

            seedIfMissing("LEAVE_DEFAULT", "Đăng ký nghỉ phép (mặc định)", "LEAVE",
                    "Đơn xin nghỉ phép qua 2 tầng duyệt.",
                    List.of(
                            step("Quản lý trực tiếp duyệt", "MANAGER", null, "Manager của người xin nghỉ xem xét bối cảnh công việc"),
                            step("HR duyệt", "ROLE", "HR", "HR verify quỹ phép, chính sách công ty")
                    ));

            seedIfMissing("CONTRACT_APPROVE", "Duyệt hợp đồng (mặc định)", "CONTRACT",
                    "3 tầng duyệt cho hợp đồng: Legal → Finance → CEO.",
                    List.of(
                            step("Legal review", "ROLE", "LEGAL", "Kiểm tra điều khoản pháp lý"),
                            step("Finance review", "ROLE", "FINANCE", "Kiểm tra tài chính, thanh toán"),
                            step("CEO ký", "ADMIN", null, "Approval cuối cùng")
                    ));
        } catch (Exception ex) {
            log.warn("[wf-seed] Bỏ qua seed default workflow definitions: {}", ex.getMessage());
        }
    }

    private void seedIfMissing(String code, String name, String moduleCode, String description,
                               List<WorkflowStepDto> steps) {
        if (definitionRepository.existsByCode(code)) return;
        WorkflowDefinitionDto dto = new WorkflowDefinitionDto();
        dto.setCode(code);
        dto.setName(name);
        dto.setModuleCode(moduleCode);
        dto.setDescription(description);
        dto.setActive(true);
        dto.setSteps(new ArrayList<>(steps));
        workflowService.saveDefinition(dto);
        log.info("[wf-seed] Đã seed {}", code);
    }

    private static WorkflowStepDto step(String name, String type, String value, String desc) {
        WorkflowStepDto s = new WorkflowStepDto();
        s.setStepName(name);
        s.setApproverType(type);
        s.setApproverValue(value);
        s.setDescription(desc);
        s.setAllowSkip(false);
        return s;
    }
}
