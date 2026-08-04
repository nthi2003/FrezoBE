package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.crm.common.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_invoice",
        uniqueConstraints = @UniqueConstraint(name = "uk_crm_invoice_code", columnNames = {"code"}),
        indexes = {
                @Index(name = "idx_crm_invoice_customer", columnList = "customer_id"),
                @Index(name = "idx_crm_invoice_quote", columnList = "quote_id"),
                @Index(name = "idx_crm_invoice_status", columnList = "status")
        })
public class Invoice extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "quote_id", length = 36)
    private String quoteId;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal", precision = 20, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "total", precision = 20, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "paid_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status;

    /** Đã hạch toán sang GL (accounting.journal_entry_id). */
    @Column(name = "gl_journal_entry_id", length = 36)
    private String glJournalEntryId;

    @Column(length = 2000)
    private String notes;

    /** Sale phụ trách nhận hoa hồng (username). */
    @Column(name = "salesperson_username", length = 80)
    private String salespersonUsername;

    /** % hoa hồng áp dụng cho HĐ này (EU có thể override). */
    @Column(name = "commission_rate_percent", precision = 8, scale = 4)
    private BigDecimal commissionRatePercent;

    /** Số tiền hoa hồng ước tính = total × rate / 100. */
    @Column(name = "commission_amount", precision = 20, scale = 2)
    private BigDecimal commissionAmount;
}
