package com.frezo.customer.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Voucher / Khuyến mãi.
 * User tự tạo và cấu hình qua UI – không cần dev can thiệp.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "vouchers")
public class Voucher extends BaseEntity {

    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /** PERCENT | FIXED */
    @Column(name = "discount_type", length = 20)
    private String discountType;

    /** Giá trị giảm (% hoặc số tiền cố định) */
    @Column(name = "discount_value", precision = 18, scale = 2)
    private BigDecimal discountValue;

    /** Giá trị đơn hàng tối thiểu để áp dụng */
    @Column(name = "min_order_value", precision = 18, scale = 2)
    private BigDecimal minOrderValue;

    /** Số lần sử dụng tối đa (null = không giới hạn) */
    @Column(name = "max_usage")
    private Integer maxUsage;

    /** Số lần đã dùng */
    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** ACTIVE | INACTIVE | EXPIRED */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "description", length = 1000)
    private String description;
}
