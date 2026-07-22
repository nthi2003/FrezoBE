package com.frezo.qlns.service.impl.payroll;

import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.engine.PayrollEngine;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.entity.PayrollDetail;
import com.frezo.qlns.mapper.PayrollDetailMapper;
import com.frezo.qlns.repository.PayrollDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Chịu trách nhiệm ghi/đọc {@link PayrollDetail} — các dòng chi tiết của phiếu lương.
 * Tách khỏi service chính để giảm dep count.
 */
@Component
@RequiredArgsConstructor
public class PayrollDetailWriter {

    private final PayrollDetailRepository payrollDetailRepository;
    private final PayrollDetailMapper payrollDetailMapper;

    /**
     * Xoá toàn bộ detail cũ của phiếu và ghi lại theo {@code result} — idempotent với recompute.
     */
    @Transactional
    public void saveAll(Payroll payroll, PayrollEngine.PayrollResult result) {
        payrollDetailRepository.deleteByPayrollId(payroll.getId());

        List<PayrollDetail> details = new ArrayList<>();
        int order = 1;

        details.add(build(payroll.getId(), "BASIC_SALARY", "Lương cơ bản", "EARNING",
                result.getBasicSalary(), order++));
        details.add(build(payroll.getId(), "ACTUAL_SALARY", "Lương theo công thực tế", "EARNING",
                result.getActualSalary(), order++));
        details.add(build(payroll.getId(), "OT_NORMAL", "Lương OT ngày thường", "EARNING",
                result.getOvertimePay(), order++));
        details.add(build(payroll.getId(), "ALLOWANCE", "Phụ cấp", "EARNING",
                result.getAllowance(), order++));
        details.add(build(payroll.getId(), "BONUS", "Thưởng", "EARNING",
                result.getBonus(), order++));
        details.add(build(payroll.getId(), "LATE_PENALTY", "Phạt đi muộn", "DEDUCTION",
                result.getLatePenalty(), order++));
        details.add(build(payroll.getId(), "SOCIAL_INSURANCE", "BHXH", "DEDUCTION",
                result.getSocialInsurance(), order++));
        details.add(build(payroll.getId(), "HEALTH_INSURANCE", "BHYT", "DEDUCTION",
                result.getHealthInsurance(), order++));
        details.add(build(payroll.getId(), "UNEMPLOYMENT_INSURANCE", "BHTN", "DEDUCTION",
                result.getUnemploymentInsurance(), order++));
        details.add(build(payroll.getId(), "UNION_FEE", "Công đoàn phí", "DEDUCTION",
                result.getUnionFee(), order++));
        details.add(build(payroll.getId(), "TAX_INCOME", "Thuế TNCN", "DEDUCTION",
                result.getTaxIncome(), order++));
        details.add(build(payroll.getId(), "ADVANCE", "Tạm ứng", "DEDUCTION",
                result.getAdvanceDeduction(), order++));
        details.add(build(payroll.getId(), "OTHER_DEDUCTION", "Khấu trừ khác", "DEDUCTION",
                result.getOtherDeductions(), order++));
        details.add(build(payroll.getId(), "NET_SALARY", "Thực lĩnh", "NET",
                result.getNetSalary(), order));

        payrollDetailRepository.saveAll(details);
    }

    /** Trả danh sách dòng chi tiết của 1 phiếu lương (đã sort theo sortOrder). */
    public List<PayrollDetailResponse> getDetails(String payrollId) {
        return payrollDetailRepository.findByPayrollIdOrderBySortOrderAsc(payrollId)
                .stream().map(payrollDetailMapper::toResponse).toList();
    }

    private PayrollDetail build(String payrollId, String code, String name, String type,
                                BigDecimal amount, int order) {
        return PayrollDetail.builder()
                .payrollId(payrollId)
                .componentCode(code)
                .componentName(name)
                .componentType(type)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .sortOrder(order)
                .build();
    }
}
