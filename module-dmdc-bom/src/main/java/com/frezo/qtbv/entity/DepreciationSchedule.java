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
    @Column(name = "method", length = 30, nullable = false)
    private String method;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "months", nullable = false)
    private Integer months;
    @Column(name = "monthly_amount", precision = 18, scale = 2)
    private BigDecimal monthlyAmount;
    @Column(name = "remaining_value", precision = 18, scale = 2)
    private BigDecimal remainingValue;
    @Column(name = "status", length = 20)
    private String status;
}
