package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "token_redeem_request", indexes = {
        @Index(name = "idx_token_redeem_person", columnList = "person_id"),
        @Index(name = "idx_token_redeem_status", columnList = "status"),
        @Index(name = "idx_token_redeem_period", columnList = "target_year, target_month, status")
})
public class TokenRedeemRequest extends BaseEntity {

    @Column(name = "person_id", length = 36, nullable = false)
    private String personId;

    /** Số token đổi. */
    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** amount × tokenToVnd lúc tạo / duyệt. */
    @Column(name = "cash_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal cashValue;

    @Column(name = "note", length = 1000)
    private String note;

    /** PENDING | APPROVED | REJECTED | PAID */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "payroll_period_id", length = 36)
    private String payrollPeriodId;

    /** Kỳ lương đích để cộng bonus (sau khi APPROVED). */
    @Column(name = "target_month")
    private Integer targetMonth;

    @Column(name = "target_year")
    private Integer targetYear;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;
}
