package com.frezo.qlns.service.Impl;

import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.dto.request.JournalEntryRequest;
import com.frezo.accounting.dto.request.JournalLineRequest;
import com.frezo.accounting.entity.AccountingSetting;
import com.frezo.accounting.repository.JournalEntryRepository;
import com.frezo.accounting.service.AccountingSettingService;
import com.frezo.accounting.service.JournalService;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.repository.PayrollRepository;
import com.frezo.qlns.service.PayrollGLPostingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Sinh bút toán aggregate cho toàn bộ payroll của 1 kỳ (month/year).
 * <p>Chiến lược tổng quát (bút toán chuẩn VN):
 * <pre>
 * Nợ 642x (Chi phí lương)                = tổng grossSalary + phần BHXH/BHYT/BHTN của DN
 *   Có 334 (Phải trả CBCNV)             = tổng netSalary + phần BH của employee + PIT
 *   Có 3383 (BHXH - phần employee)      = tổng socialInsurance
 *   Có 3384 (BHYT - phần employee)      = tổng healthInsurance
 *   Có 3385/3386 (BHTN - phần employee) = tổng unemploymentInsurance
 *   Có 3335 (Thuế TNCN khấu trừ)        = tổng taxIncome
 *   Có 3382 (KPCĐ)                      = tổng unionFee
 * </pre>
 * <p>Aggregate — tất cả nhân viên gộp thành 1 bút toán để GL sạch, chi tiết vẫn ở Payroll subledger.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollGLPostingServiceImpl implements PayrollGLPostingService {

    private final PayrollRepository payrollRepo;
    private final JournalService journalService;
    private final JournalEntryRepository journalRepo;
    private final AccountingSettingService settingService;

    @Override
    @Transactional
    public String postPeriod(Integer month, Integer year) {
        String idemKey = idempotencyKey(month, year);
        var existing = journalRepo.findByIdempotencyKey(idemKey);
        if (existing.isPresent()) {
            log.info("Payroll period {}/{} already posted → returning existing entry {}",
                    month, year, existing.get().getCode());
            return existing.get().getId();
        }

        List<Payroll> payrolls = payrollRepo.findByMonthAndYearAndIsDeletedFalse(month, year);
        if (payrolls.isEmpty()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST,
                    "Không có bảng lương nào trong kỳ " + month + "/" + year);
        }

        AccountingSetting st = settingService.getOrCreateDefault();

        BigDecimal totalGross = sum(payrolls, Payroll::getGrossSalary);
        BigDecimal totalNet = sum(payrolls, Payroll::getNetSalary);
        BigDecimal totalBhxh = sum(payrolls, Payroll::getSocialInsurance);
        BigDecimal totalBhyt = sum(payrolls, Payroll::getHealthInsurance);
        BigDecimal totalBhtn = sum(payrolls, Payroll::getUnemploymentInsurance);
        BigDecimal totalPit = sum(payrolls, Payroll::getTaxIncome);
        BigDecimal totalUnion = sum(payrolls, Payroll::getUnionFee);

        // Cross-check: gross ~= net + bhxh + bhyt + bhtn + pit (bỏ qua allowance/bonus deduction chi tiết)
        // Nếu lệch, log warning; vẫn post theo số thực để cân đối bằng phần chênh (allowance/bonus/late...)
        BigDecimal debit = totalGross;
        BigDecimal creditSum = totalNet.add(totalBhxh).add(totalBhyt).add(totalBhtn)
                .add(totalPit).add(totalUnion);
        BigDecimal diff = debit.subtract(creditSum);
        if (diff.signum() != 0) {
            log.warn("Payroll GL posting {}/{} — mismatch debit vs sum of credits, diff = {}. " +
                    "Phần chênh sẽ dồn vào TK 334 để cân đối.", month, year, diff);
            totalNet = totalNet.add(diff);
        }

        List<JournalLineRequest> lines = new ArrayList<>();
        JournalLineRequest expenseLine = new JournalLineRequest();
        expenseLine.setAccountCode(st.getAccSalaryExpense() != null ? st.getAccSalaryExpense() : "6421");
        expenseLine.setDebit(totalGross);
        expenseLine.setCredit(BigDecimal.ZERO);
        expenseLine.setDescription("Lương gộp kỳ " + month + "/" + year);
        lines.add(expenseLine);

        if (totalNet.signum() > 0) {
            lines.add(credit(st.getAccSalaryPayable() != null ? st.getAccSalaryPayable() : "334",
                    totalNet, "Phải trả CBCNV kỳ " + month + "/" + year));
        }
        if (totalBhxh.signum() > 0) {
            lines.add(credit(st.getAccBhxhPayable() != null ? st.getAccBhxhPayable() : "3383",
                    totalBhxh, "BHXH khấu trừ CBCNV"));
        }
        if (totalBhyt.signum() > 0) {
            lines.add(credit(st.getAccBhytPayable() != null ? st.getAccBhytPayable() : "3384",
                    totalBhyt, "BHYT khấu trừ CBCNV"));
        }
        if (totalBhtn.signum() > 0) {
            lines.add(credit(st.getAccBhtnPayable() != null ? st.getAccBhtnPayable() : "3385",
                    totalBhtn, "BHTN khấu trừ CBCNV"));
        }
        if (totalPit.signum() > 0) {
            lines.add(credit(st.getAccPitPayable() != null ? st.getAccPitPayable() : "3335",
                    totalPit, "Thuế TNCN khấu trừ"));
        }
        if (totalUnion.signum() > 0) {
            lines.add(credit(st.getAccUnionFee() != null ? st.getAccUnionFee() : "3382",
                    totalUnion, "Kinh phí công đoàn"));
        }

        JournalEntryRequest req = new JournalEntryRequest();
        YearMonth ym = YearMonth.of(year, month);
        req.setPostingDate(ym.atEndOfMonth());
        req.setDocumentDate(ym.atEndOfMonth());
        req.setDescription(String.format("Hạch toán bảng lương kỳ %02d/%d (%d NV)",
                month, year, payrolls.size()));
        req.setSourceType(PostingSource.PAYROLL);
        req.setSourceId(month + "-" + year);
        req.setIdempotencyKey(idemKey);
        req.setLines(lines);

        var response = journalService.createAndPost(req);
        log.info("Payroll period {}/{} posted to GL as journal {}", month, year, response.getCode());
        return response.getId();
    }

    @Override
    @Transactional
    public String reversePeriod(Integer month, Integer year, String reason) {
        var existing = journalRepo.findByIdempotencyKey(idempotencyKey(month, year))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND,
                        "Chưa có bút toán payroll cho kỳ " + month + "/" + year));
        return journalService.reverse(existing.getId(), reason).getId();
    }

    private JournalLineRequest credit(String code, BigDecimal amount, String desc) {
        JournalLineRequest l = new JournalLineRequest();
        l.setAccountCode(code);
        l.setDebit(BigDecimal.ZERO);
        l.setCredit(amount);
        l.setDescription(desc);
        return l;
    }

    private static BigDecimal sum(List<Payroll> payrolls, java.util.function.Function<Payroll, BigDecimal> f) {
        BigDecimal s = BigDecimal.ZERO;
        for (Payroll p : payrolls) {
            BigDecimal v = f.apply(p);
            if (v != null) s = s.add(v);
        }
        return s;
    }

    private static String idempotencyKey(Integer month, Integer year) {
        return String.format("payroll:%04d-%02d", year, month);
    }

    private LocalDate today() {
        return LocalDate.now();
    }
}
