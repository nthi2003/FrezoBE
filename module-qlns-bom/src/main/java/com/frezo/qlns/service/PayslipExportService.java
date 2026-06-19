package com.frezo.qlns.service;

import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.dto.response.PayrollResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayslipExportService {

    private final PayrollService payrollService;

    public byte[] exportPayslip(String payrollId) {
        PayrollResponse payroll = payrollService.getById(payrollId);
        List<PayrollDetailResponse> details = payrollService.getPayrollDetails(payrollId);

        StringBuilder sb = new StringBuilder();
        sb.append("PHIẾU LƯƠNG\n");
        sb.append("Tháng ").append(payroll.getPayMonth()).append("/").append(payroll.getPayYear()).append("\n");
        sb.append("=".repeat(50)).append("\n\n");

        sb.append("Lương cơ bản: ").append(format(payroll.getBaseSalary())).append("\n");
        sb.append("Ngày công chuẩn: ").append(payroll.getStandardDays()).append("\n");
        sb.append("Ngày công thực tế: ").append(payroll.getActualWorkingDays()).append("\n");
        sb.append("-".repeat(50)).append("\n");

        sb.append("CÁC KHOẢN THU NHẬP:\n");
        for (PayrollDetailResponse d : details) {
            if ("EARNING".equals(d.getComponentType())) {
                sb.append("  ").append(d.getComponentName()).append(": ").append(format(d.getAmount())).append("\n");
            }
        }

        sb.append("\nCÁC KHOẢN KHẤU TRỪ:\n");
        for (PayrollDetailResponse d : details) {
            if ("DEDUCTION".equals(d.getComponentType())) {
                sb.append("  ").append(d.getComponentName()).append(": ").append(format(d.getAmount())).append("\n");
            }
        }

        sb.append("-".repeat(50)).append("\n");
        sb.append("TỔNG THU NHẬP: ").append(format(payroll.getGrossSalary())).append("\n");
        sb.append("TỔNG KHẤU TRỪ: ").append(format(payroll.getTotalDeductions())).append("\n");
        sb.append("THỰC LĨNH: ").append(format(payroll.getNetSalary())).append("\n");

        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public byte[] exportBankPayment(List<String> payrollIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH CHI LƯƠNG\n");
        sb.append("Ngày: ").append(java.time.LocalDate.now()).append("\n");
        sb.append("=".repeat(60)).append("\n\n");
        sb.append(String.format("%-5s %-30s %-15s %-15s\n", "STT", "Họ tên", "Số TK", "Thực lĩnh"));
        sb.append("-".repeat(60)).append("\n");

        BigDecimal total = BigDecimal.ZERO;
        int i = 1;
        for (String pid : payrollIds) {
            PayrollResponse p = payrollService.getById(pid);
            sb.append(String.format("%-5d %-30s %-15s %-15s\n", i++, p.getPersonId(), "", format(p.getNetSalary())));
            total = total.add(p.getNetSalary() != null ? p.getNetSalary() : BigDecimal.ZERO);
        }

        sb.append("-".repeat(60)).append("\n");
        sb.append(String.format("%-50s %-15s\n", "TỔNG CỘNG", format(total)));
        return sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String format(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }
}
