package com.frezo.qlns.service.impl.payroll;

import com.frezo.common.exception.QTHTException;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.repository.PayrollPeriodRepository;
import com.frezo.qlns.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Xử lý vòng đời (confirm/pay/delete/updateBonus) và validate lock cho {@link Payroll}.
 * Không đụng tới engine — chỉ mutation trạng thái + total.
 */
@Component
@RequiredArgsConstructor
public class PayrollLifecycleService {

    private final PayrollRepository payrollRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;

    /**
     * Chặn thay đổi phiếu khi kỳ đã khoá (period.status ≥ 1) hoặc phiếu đã confirmed/paid.
     * Được orchestrator gọi trước khi ghi lại payroll đã tính.
     */
    public void assertNotLocked(Payroll payroll) {
        if (payroll.getPayrollPeriodId() != null) {
            payrollPeriodRepository.findById(payroll.getPayrollPeriodId()).ifPresent(period -> {
                if (period.getStatus() == 1 || period.getStatus() == 2) {
                    throw new QTHTException("Không thể thay đổi bảng lương trong kỳ đã khóa");
                }
            });
        }
        if (payroll.getStatus() != null && payroll.getStatus() > 0) {
            throw new QTHTException("error.payroll.cannot.update");
        }
    }

    @Transactional
    public Payroll updateBonus(String id, Double bonus, Double deduction, String note) {
        Payroll p = findOrThrow(id);
        assertNotLocked(p);

        p.setBonus(BigDecimal.valueOf(bonus));
        p.setOtherDeductions(BigDecimal.valueOf(deduction));
        p.setNote(note);
        PayrollCalculationHelper.recalculateTotals(p);
        return payrollRepository.save(p);
    }

    @Transactional
    public Payroll confirm(String id) {
        Payroll p = findOrThrow(id);
        if (p.getStatus() != 0) throw new QTHTException("error.payroll.cannot.update");
        p.setStatus(1);
        return payrollRepository.save(p);
    }

    @Transactional
    public Payroll pay(String id) {
        Payroll p = findOrThrow(id);
        if (p.getStatus() != 1) throw new QTHTException("Chỉ có thể thanh toán bảng lương đã xác nhận");
        p.setStatus(2);
        p.setPaidAt(LocalDate.now());
        return payrollRepository.save(p);
    }

    @Transactional
    public void softDelete(String id) {
        Payroll p = findOrThrow(id);
        assertNotLocked(p);
        p.setIsDeleted(true);
        payrollRepository.save(p);
    }

    private Payroll findOrThrow(String id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.payroll.not.found"));
    }
}
