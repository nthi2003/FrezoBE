package com.frezo.qlns.service.impl.payroll;

import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolve payroll record cho 1 nhân viên / kỳ — xử lý trùng bản ghi do seed multi-contract
 * hoặc chạy lại script SQL (NonUniqueResultException trên {@code Optional} query cũ).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollRecordResolver {

    private final PayrollRepository payrollRepository;

    /**
     * Trả về bản ghi lương duy nhất (non-deleted) mới nhất, hoặc shell entity nếu chưa có.
     * Bản trùng (cùng personId + month + year) được soft-delete — giữ bản {@code updatedDate} mới nhất.
     */
    public Payroll resolveForCalculation(String personId, Integer month, Integer year) {
        List<Payroll> records = payrollRepository
                .findByPersonIdAndMonthAndYearAndIsDeletedFalseOrderByUpdatedDateDescIdDesc(
                        personId, month, year);

        if (records.isEmpty()) {
            return Payroll.builder()
                    .personId(personId)
                    .month(month)
                    .year(year)
                    .build();
        }

        Payroll primary = records.get(0);
        if (records.size() > 1) {
            for (int i = 1; i < records.size(); i++) {
                Payroll duplicate = records.get(i);
                duplicate.setIsDeleted(true);
                payrollRepository.save(duplicate);
                log.warn("Removed duplicate payroll id={} for person={} {}/{} (kept id={})",
                        duplicate.getId(), personId, month, year, primary.getId());
            }
        }
        return primary;
    }
}
