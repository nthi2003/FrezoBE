package com.frezo.qtbv.entity;

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
import java.time.LocalDate;

/**
 * Lịch khấu hao TSCĐ — mỗi Asset có tối đa 1 schedule ACTIVE.
 * <p>Method:
 * <ul>
 *   <li>{@code STRAIGHT_LINE} — chia đều {@code purchasePrice / months}.</li>
 *   <li>{@code DECLINING} — giảm dần (chưa dùng ở v1, để field mở rộng sau).</li>
 * </ul>
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "depreciation_schedule", indexes = {
        @Index(name = "idx_dep_sched_asset", columnList = "asset_id", unique = true)
})
public class DepreciationSchedule extends BaseEntity {

    @Column(name = "asset_id", length = 36, nullable = false)
    private String assetId;

    /** STRAIGHT_LINE / DECLINING. */
    @Column(name = "method", length = 30, nullable = false)
    private String method;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Tổng số tháng khấu hao (VD 36 tháng cho IT). */
    @Column(name = "months", nullable = false)
    private Integer months;

    /** Số khấu hao mỗi tháng (làm tròn tháng cuối để cân đối). */
    @Column(name = "monthly_amount", precision = 18, scale = 2)
    private BigDecimal monthlyAmount;

    /** Giá trị còn lại (server tự cập nhật sau mỗi kỳ posting). */
    @Column(name = "remaining_value", precision = 18, scale = 2)
    private BigDecimal remainingValue;

    /** ACTIVE / DONE / CANCELLED. */
    @Column(name = "status", length = 20)
    private String status;
}
