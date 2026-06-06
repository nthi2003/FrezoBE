package com.frezo.cms.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "voucher")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vouchers extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "discount_type", length = 20)
    private String discountType; // PERCENTAGE, FIXED_AMOUNT

    @Column(name = "discount_value")
    private BigDecimal discountValue; 

    @Column(name = "min_order_value")
    private BigDecimal minOrderValue; // Áp dụng cho đơn từ bao nhiêu tiền

    @Column(name = "max_discount_amount")
    private BigDecimal maxDiscountAmount; // Số tiền giảm tối đa (nếu giảm theo % )

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit; // Tổng số lượt sử dụng voucher

    @Column(name = "used_count")
    private Integer usedCount; // Số lượt đã sử dụng

    @Column(name = "is_active")
    private Boolean isActive;
}
