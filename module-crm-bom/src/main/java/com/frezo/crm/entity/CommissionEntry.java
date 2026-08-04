package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bản ghi hoa hồng phát sinh từ hoá đơn (khi thu tiền / PAID).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_commission_entry",
        indexes = {
                @Index(name = "idx_crm_commission_entry_sale", columnList = "salesperson_username"),
                @Index(name = "idx_crm_commission_entry_invoice", columnList = "invoice_id"),
                @Index(name = "idx_crm_commission_entry_status", columnList = "status")
        })
public class CommissionEntry extends BaseEntity {

    @Column(name = "invoice_id", nullable = false, length = 36)
    private String invoiceId;

    @Column(name = "invoice_code", length = 50)
    private String invoiceCode;

    @Column(name = "deal_id", length = 36)
    private String dealId;

    @Column(name = "salesperson_username", nullable = false, length = 80)
    private String salespersonUsername;

    /** Doanh số làm cơ sở (thường = invoice.total hoặc paid_amount). */
    @Column(name = "base_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal baseAmount;

    @Column(name = "rate_percent", precision = 8, scale = 4, nullable = false)
    private BigDecimal ratePercent;

    @Column(name = "commission_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal commissionAmount;

    /** Số dòng / tổng SL sản phẩm trên HĐ (để EU xem “số lượng + hoa hồng”). */
    @Column(name = "item_quantity", precision = 20, scale = 4)
    private BigDecimal itemQuantity;

    /** PENDING | APPROVED | PAID | VOID */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "accrued_at")
    private LocalDateTime accruedAt;

    @Column(length = 1000)
    private String notes;
}
