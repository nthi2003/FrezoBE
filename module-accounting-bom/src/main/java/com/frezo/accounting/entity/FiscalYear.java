package com.frezo.accounting.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Năm tài chính. Mặc định 01/01 - 31/12 theo Luật Kế toán VN,
 * cho phép custom nếu công ty áp dụng năm tài chính lệch (rất hiếm ở VN).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "acc_fiscal_year",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_fiscal_year_code", columnNames = {"code"}))
public class FiscalYear extends BaseEntity {

    /** Mã năm (VD: "2026"). */
    @Column(name = "code", nullable = false, length = 10)
    private String code;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Đã quyết toán — khoá vĩnh viễn. */
    @Column(name = "closed", nullable = false)
    private Boolean closed;
}
