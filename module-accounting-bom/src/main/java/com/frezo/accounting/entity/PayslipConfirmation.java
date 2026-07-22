package com.frezo.accounting.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Ghi nhận xác nhận đã nhận payslip từ nhân viên (Mobile app).
 * Có thể xem như "chữ ký điện tử nhẹ" — không phải chữ ký số, chỉ chứng minh
 * người dùng đã đăng nhập vào app và bấm confirm với timestamp + IP.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "acc_payslip_confirmation",
        uniqueConstraints = @UniqueConstraint(name = "uk_payslip_conf_payroll",
                columnNames = {"payroll_id"}))
public class PayslipConfirmation extends BaseEntity {

    @Column(name = "payroll_id", nullable = false, length = 36)
    private String payrollId;

    @Column(name = "person_id", nullable = false, length = 36)
    private String personId;

    @Column(name = "confirmed_at", nullable = false)
    private java.time.LocalDateTime confirmedAt;

    @Column(name = "confirmed_from_ip", length = 45)
    private String confirmedFromIp;

    @Column(name = "confirmed_from_device", length = 100)
    private String confirmedFromDevice;

    /** Ghi chú tự do từ nhân viên (khiếu nại, ý kiến...). */
    @Column(name = "note", length = 1000)
    private String note;
}
