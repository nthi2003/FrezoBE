package com.frezo.customer.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Trạng thái thanh toán của khách hàng.
 * Lưu từng lần giao dịch (invoice / payment).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customer_payments")
public class CustomerPayment extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    /** Mã hóa đơn / mã giao dịch */
    @Column(name = "invoice_code", length = 100)
    private String invoiceCode;

    @Column(name = "amount", nullable = false, precision = 18, scale = 0)
    private BigDecimal amount;

    /** PENDING | PAID | PARTIAL | OVERDUE | CANCELLED */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "note", length = 1000)
    private String note;
}
