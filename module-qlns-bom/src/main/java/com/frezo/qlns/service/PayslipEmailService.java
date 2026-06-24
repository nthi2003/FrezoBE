package com.frezo.qlns.service;

import com.frezo.email.service.EmailService;
import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayslipEmailService {

    private final PayrollService payrollService;
    private final PersonRepository personRepository;
    private final EmailService emailService;

    public void sendPayslipToEmail(String payrollId) {
        PayrollResponse payroll = payrollService.getById(payrollId);
        List<PayrollDetailResponse> details = payrollService.getPayrollDetails(payrollId);

        Person person = personRepository.findById(payroll.getPersonId()).orElse(null);
        if (person == null || person.getEmail() == null || person.getEmail().isBlank()) {
            log.warn("No email found for person {}", payroll.getPersonId());
            return;
        }

        String html = buildPayslipHtml(payroll, details, person.getName());
        String subject = "Phiếu lương tháng " + payroll.getPayMonth() + "/" + payroll.getPayYear();

        emailService.sendBulk(com.frezo.email.dto.request.BulkEmailRequest.builder()
                .subject(subject)
                .body(html)
                .recipients(List.of(person.getEmail()))
                .description("Phiếu lương " + payrollId)
                .build());
    }

    public void sendPayslipBatch(List<String> payrollIds) {
        for (String id : payrollIds) {
            try {
                sendPayslipToEmail(id);
            } catch (Exception e) {
                log.error("Failed to send payslip {}: {}", id, e.getMessage());
            }
        }
    }

    private String buildPayslipHtml(PayrollResponse p, List<PayrollDetailResponse> details, String employeeName) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
            <div style="font-family:sans-serif;max-width:600px;margin:auto;padding:20px;border:1px solid #ddd;border-radius:8px">
            <h2 style="text-align:center;color:#1a73e8">PHIẾU LƯƠNG</h2>
            <p style="text-align:center">Tháng %d/%d</p>
            <hr>
            <p><b>Nhân viên:</b> %s</p>
            <table style="width:100%%;border-collapse:collapse">
                <tr><td>Lương cơ bản</td><td style="text-align:right">%s</td></tr>
                <tr><td>Ngày công chuẩn</td><td style="text-align:right">%d</td></tr>
                <tr><td>Ngày công thực tế</td><td style="text-align:right">%s</td></tr>
            </table>
            <hr>
            <h3 style="color:#2e7d32">Thu nhập</h3>
            <table style="width:100%%;border-collapse:collapse">
            """.formatted(p.getPayMonth(), p.getPayYear(), employeeName,
                    fmt(p.getBaseSalary()), p.getStandardDays(), fmt(p.getActualWorkingDays())));

        for (PayrollDetailResponse d : details) {
            if ("EARNING".equals(d.getComponentType())) {
                sb.append("<tr><td>").append(d.getComponentName()).append("</td>")
                  .append("<td style='text-align:right'>").append(fmt(d.getAmount())).append("</td></tr>\n");
            }
        }

        sb.append("""
            </table>
            <h3 style="color:#c62828">Khấu trừ</h3>
            <table style="width:100%%;border-collapse:collapse">
            """);

        for (PayrollDetailResponse d : details) {
            if ("DEDUCTION".equals(d.getComponentType())) {
                sb.append("<tr><td>").append(d.getComponentName()).append("</td>")
                  .append("<td style='text-align:right'>").append(fmt(d.getAmount())).append("</td></tr>\n");
            }
        }

        sb.append("""
            </table>
            <hr>
            <table style="width:100%%;border-collapse:collapse;font-size:16px">
                <tr style="font-weight:bold"><td>TỔNG THU NHẬP</td><td style="text-align:right;color:#1a73e8">%s</td></tr>
                <tr style="font-weight:bold"><td>TỔNG KHẤU TRỪ</td><td style="text-align:right;color:#c62828">%s</td></tr>
                <tr style="font-weight:bold;font-size:18px"><td>THỰC LĨNH</td><td style="text-align:right;color:#2e7d32">%s</td></tr>
            </table>
            <hr>
            <p style="text-align:center;color:#999;font-size:12px">Phiếu lương được tạo tự động từ hệ thống Frezo</p>
            </div>
            """.formatted(fmt(p.getGrossSalary()), fmt(p.getTotalDeductions()), fmt(p.getNetSalary())));

        return sb.toString();
    }

    private String fmt(BigDecimal v) {
        if (v == null) return "0";
        return String.format("%,.0f", v) + " VNĐ";
    }
}
