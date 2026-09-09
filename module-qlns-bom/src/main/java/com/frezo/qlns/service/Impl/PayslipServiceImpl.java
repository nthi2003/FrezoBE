package com.frezo.qlns.service.Impl;

import com.frezo.accounting.entity.PayslipConfirmation;
import com.frezo.accounting.service.PayslipConfirmationService;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.dto.response.PayrollYtdResponse;
import com.frezo.qlns.dto.response.PayslipFormulaResponse;
import com.frezo.qlns.dto.response.PayslipResponse;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.repository.PayrollRepository;
import com.frezo.qlns.service.PayrollService;
import com.frezo.qlns.service.PayslipService;
import com.frezo.qtht.entity.Department;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.DepartmentRepository;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PayslipServiceImpl implements PayslipService {

    private final PayrollRepository payrollRepo;
    private final PayrollService payrollService;
    private final PersonRepository personRepo;
    private final DepartmentRepository departmentRepo;
    private final PayslipConfirmationService confirmationService;

    /**
     * Lấy phiếu lương của một kỳ lương theo ID, kèm thông tin nhân viên, khoản thu nhập, khấu trừ và trạng thái đã xác nhận.
     */
    @Override
    @Transactional(readOnly = true)
    public PayslipResponse getPayslip(String payrollId) {
        Payroll p = payrollRepo.findById(payrollId)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, payrollId));
        return buildPayslip(p, payrollService.getPayrollDetails(payrollId));
    }

    /**
     * Tổng hợp lũy kế theo năm (YTD) của một nhân viên: tổng gross, net, bảo hiểm, thuế TNCN theo từng tháng trong năm.
     */
    @Override
    @Transactional(readOnly = true)
    public PayrollYtdResponse getYtd(String personId, Integer year) {
        List<Payroll> yearPayrolls = payrollRepo
                .findByPersonIdAndYearAndIsDeletedFalseOrderByMonthAsc(personId, year);
        Map<Integer, Payroll> byMonth = new HashMap<>();
        for (Payroll p : yearPayrolls) byMonth.put(p.getMonth(), p);

        List<PayrollYtdResponse.Month> months = new ArrayList<>();
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalIns = BigDecimal.ZERO;
        BigDecimal totalPit = BigDecimal.ZERO;

        for (int m = 1; m <= 12; m++) {
            Payroll p = byMonth.get(m);
            BigDecimal gross = safe(p != null ? p.getGrossSalary() : null);
            BigDecimal net = safe(p != null ? p.getNetSalary() : null);
            BigDecimal ins = safe(p != null ? p.getTotalInsurance() : null);
            BigDecimal pit = safe(p != null ? p.getTaxIncome() : null);
            months.add(PayrollYtdResponse.Month.builder()
                    .month(m).gross(gross).net(net).insurance(ins).pit(pit)
                    .processed(p != null)
                    .build());
            totalGross = totalGross.add(gross);
            totalNet = totalNet.add(net);
            totalIns = totalIns.add(ins);
            totalPit = totalPit.add(pit);
        }

        return PayrollYtdResponse.builder()
                .personId(personId)
                .year(year)
                .ytdGross(totalGross)
                .ytdInsurance(totalIns)
                .ytdPit(totalPit)
                .ytdNet(totalNet)
                .monthsProcessed(yearPayrolls.size())
                .months(months)
                .build();
    }

    /**
     * Trả về danh sách công thức tính lương mô tả các khoản thu nhập/khoản khấu trừ để hiển thị hướng dẫn cho người dùng.
     */
    @Override
    public PayslipFormulaResponse getFormulas() {
        List<PayslipFormulaResponse.Formula> list = new ArrayList<>();
        list.add(f("basicSalary", "Lương cơ bản", "Từ hợp đồng lao động",
                "Mức lương thoả thuận trong HĐLĐ, dùng làm gốc tính OT, BH và thuế TNCN."));
        list.add(f("salaryBySalaryDays", "Lương theo ngày công thực tế",
                "basicSalary × (workingDays / standardDays)",
                "Trả theo số ngày làm việc thực tế trong tháng."));
        list.add(f("overtimePay", "Tiền tăng ca (OT)",
                "OT thường ×150% + OT cuối tuần ×200% + OT lễ Tết ×300%",
                "Theo BLLĐ 2019 điều 98."));
        list.add(f("allowance", "Phụ cấp", "Cộng dồn từ cấu hình phụ cấp",
                "Ăn ca, xăng xe, điện thoại, thâm niên... Có thể chịu hoặc không chịu thuế TNCN."));
        list.add(f("grossSalary", "Tổng thu nhập (Gross)",
                "salary + allowance + bonus + overtimePay",
                "Tổng thu nhập trước khi trừ các khoản BHXH/BHYT/BHTN/PIT."));
        list.add(f("socialInsurance", "BHXH", "salaryInsurance × 8%",
                "Nhân viên đóng 8%. Doanh nghiệp đóng thêm 17.5% (hạch toán riêng vào chi phí)."));
        list.add(f("healthInsurance", "BHYT", "salaryInsurance × 1.5%",
                "Nhân viên đóng 1.5%. Doanh nghiệp đóng thêm 3%."));
        list.add(f("unemploymentInsurance", "BHTN", "salaryInsurance × 1%",
                "Nhân viên đóng 1%. Doanh nghiệp đóng thêm 1%."));
        list.add(f("taxableIncome", "Thu nhập chịu thuế TNCN",
                "gross - BH nhân viên - Giảm trừ bản thân (11 triệu) - Giảm trừ người phụ thuộc (4.4 triệu/người)",
                "Áp dụng biểu thuế luỹ tiến từng phần 7 bậc."));
        list.add(f("taxIncome", "Thuế TNCN",
                "Bậc 1: 5% (≤5M) | Bậc 2: 10% (5-10M) | Bậc 3: 15% (10-18M) | Bậc 4: 20% (18-32M) | Bậc 5: 25% | Bậc 6: 30% | Bậc 7: 35%",
                "Luỹ tiến từng phần theo Nghị quyết 954/2020 và Luật Thuế TNCN."));
        list.add(f("unionFee", "Kinh phí công đoàn",
                "Doanh nghiệp đóng: quỹ lương × 2%",
                "Không trừ vào lương nhân viên."));
        list.add(f("netSalary", "Thực nhận",
                "grossSalary − BHXH − BHYT − BHTN − PIT − advances − otherDeductions",
                "Số tiền thực tế nhân viên nhận về tài khoản."));
        return PayslipFormulaResponse.builder().formulas(list).build();
    }

    /**
     * Xác nhận nhân viên đã nhận lương kỳ này (ghi nhận bản xác nhận) rồi trả về phiếu lương mới cập nhật.
     */
    @Override
    @Transactional
    public PayslipResponse confirmReceived(String payrollId, String note, String ip, String device) {
        Payroll p = payrollRepo.findById(payrollId)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, payrollId));
        confirmationService.confirm(payrollId, p.getPersonId(), note, ip, device);
        return getPayslip(payrollId);
    }

    // ---------- Helpers ----------

    /**
     * Dựng đối tượng phiếu lương đầy đủ từ bản ghi Payroll và danh sách chi tiết lương, gồm thông tin nhân viên, các khoản thu nhập, khấu trừ, chấm công và trạng thái xác nhận.
     */
    private PayslipResponse buildPayslip(Payroll p, List<PayrollDetailResponse> details) {
        Optional<Person> personOpt = personRepo.findById(p.getPersonId());
        String personName = personOpt.map(Person::getName).orElse(null);
        String personCode = personOpt.map(Person::getCode).orElse(null);
        String jobTitle = personOpt.map(Person::getJobTitle).orElse(null);
        String deptName = personOpt.flatMap(person -> {
            String dId = person.getDepartmentId();
            if (dId == null) return Optional.empty();
            return departmentRepo.findById(dId).map(Department::getName);
        }).orElse(null);

        BigDecimal salaryByDays = BigDecimal.ZERO;
        if (p.getBasicSalary() != null && p.getActualWorkingDays() != null
                && p.getStandardDays() != null && p.getStandardDays() > 0) {
            salaryByDays = p.getBasicSalary()
                    .multiply(p.getActualWorkingDays())
                    .divide(BigDecimal.valueOf(p.getStandardDays()), 0, java.math.RoundingMode.HALF_UP);
        }

        PayslipResponse.EarningsSection earnings = PayslipResponse.EarningsSection.builder()
                .basicSalary(p.getBasicSalary())
                .actualWorkingDays(p.getActualWorkingDays())
                .salaryBySalaryDays(salaryByDays)
                .overtimeNormal(p.getOvertimeHoursNormal())
                .overtimeWeekend(p.getOvertimeHoursWeekend())
                .overtimeHoliday(p.getOvertimeHoursHoliday())
                .overtimePay(p.getOvertimePay())
                .allowance(p.getAllowance())
                .bonus(p.getBonus())
                .grossSalary(p.getGrossSalary())
                .build();

        PayslipResponse.DeductionsSection ded = PayslipResponse.DeductionsSection.builder()
                .socialInsurance(p.getSocialInsurance())
                .healthInsurance(p.getHealthInsurance())
                .unemploymentInsurance(p.getUnemploymentInsurance())
                .totalInsurance(p.getTotalInsurance())
                .taxableIncome(p.getTaxableIncome())
                .taxIncome(p.getTaxIncome())
                .unionFee(p.getUnionFee())
                .latePenalty(p.getLatePenalty())
                .advanceDeduction(p.getAdvanceDeduction())
                .otherDeductions(p.getOtherDeductions())
                .totalDeductions(p.getTotalDeductions())
                .build();

        PayslipResponse.AttendanceSection att = PayslipResponse.AttendanceSection.builder()
                .standardDays(p.getStandardDays())
                .workingDays(p.getWorkingDays())
                .actualWorkingDays(p.getActualWorkingDays())
                .leavesPaid(p.getLeavesPaid())
                .leavesUnpaid(p.getLeavesUnpaid())
                .totalLateMinutes(p.getTotalLateMinutes())
                .build();

        Optional<PayslipConfirmation> confOpt = confirmationService.find(p.getId());

        return PayslipResponse.builder()
                .payrollId(p.getId())
                .personId(p.getPersonId())
                .personName(personName)
                .personCode(personCode)
                .jobTitle(jobTitle)
                .departmentName(deptName)
                .contractId(p.getContractId())
                .month(p.getMonth())
                .year(p.getYear())
                .periodLabel(String.format("Tháng %02d/%d", p.getMonth(), p.getYear()))
                .netSalary(p.getNetSalary())
                .status(p.getStatus())
                .statusLabel(statusLabel(p.getStatus()))
                .paidAt(p.getPaidAt())
                .earnings(earnings)
                .deductions(ded)
                .attendance(att)
                .details(details)
                .confirmed(confOpt.isPresent())
                .confirmedAt(confOpt.map(PayslipConfirmation::getConfirmedAt).orElse(null))
                .confirmationNote(confOpt.map(PayslipConfirmation::getNote).orElse(null))
                .build();
    }

    /**
     * Tạo nhanh một đối tượng Formula chứa key, nhãn, biểu thức và giải thích cho danh sách công thức lương.
     */
    private static PayslipFormulaResponse.Formula f(String key, String label, String formula, String explain) {
        return PayslipFormulaResponse.Formula.builder()
                .key(key).label(label).formula(formula).explanation(explain).build();
    }

    /**
     * Trả về giá trị BigDecimal an toàn, thay null bằng 0 để tránh lỗi tính toán.
     */
    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * Chuyển mã trạng thái lương (0/1/2) thành nhãn tiếng Việt hiển thị trên phiếu lương.
     */
    private static String statusLabel(Integer status) {
        if (status == null) return "Chưa xác định";
        return switch (status) {
            case 0 -> "Bản nháp";
            case 1 -> "Đã xác nhận";
            case 2 -> "Đã trả";
            default -> "Trạng thái " + status;
        };
    }
}
