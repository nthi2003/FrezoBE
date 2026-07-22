package com.frezo.approval.config;

import com.frezo.approval.entity.ApprovalFlow;
import com.frezo.approval.entity.ApprovalFlowStep;
import com.frezo.approval.repository.ApprovalFlowRepository;
import com.frezo.approval.repository.ApprovalFlowStepRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Seed flow chuẩn Sprint 2: LEAVE_STANDARD, PAYROLL_PERIOD, PURCHASE_REQUEST.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class ApprovalFlowSeedRunner implements ApplicationRunner {

    private final ApprovalFlowRepository flowRepository;
    private final ApprovalFlowStepRepository flowStepRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("LEAVE_STANDARD", "Nghỉ phép chuẩn", "LEAVE",
                "Manager → HR",
                new String[][]{{"1", "MANAGER", "QL trực tiếp"}, {"2", "HR", "Nhân sự"}});
        seed("PAYROLL_PERIOD", "Khoá kỳ lương", "PAYROLL",
                "Kế toán trưởng → Admin",
                new String[][]{{"1", "CHIEF_ACC", "Kế toán trưởng"}, {"2", "ADMIN", "Admin"}});
        seed("PURCHASE_REQUEST", "Yêu cầu mua hàng", "PURCHASE_REQUEST",
                "Manager → Admin",
                new String[][]{{"1", "MANAGER", "QL mua hàng"}, {"2", "ADMIN", "Admin duyệt"}});
    }

    private void seed(String code, String name, String subjectType, String desc, String[][] steps) {
        if (flowRepository.findByCodeAndIsDeletedFalse(code).isPresent()) {
            return;
        }
        ApprovalFlow flow = ApprovalFlow.builder()
                .code(code)
                .name(name)
                .subjectType(subjectType)
                .active(true)
                .description(desc)
                .build();
        flow.setId(UUID.randomUUID().toString());
        flow = flowRepository.save(flow);
        for (String[] s : steps) {
            ApprovalFlowStep step = ApprovalFlowStep.builder()
                    .flowId(flow.getId())
                    .stepOrder(Integer.parseInt(s[0]))
                    .approverRole(s[1])
                    .name(s[2])
                    .build();
            step.setId(UUID.randomUUID().toString());
            flowStepRepository.save(step);
        }
        log.info("[approval] Seeded flow {}", code);
    }
}
