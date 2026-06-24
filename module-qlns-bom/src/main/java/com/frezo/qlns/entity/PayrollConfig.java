package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "payroll_config")
public class PayrollConfig extends BaseEntity {

    @Column(name = "org_id")
    private String orgId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "standard_working_days")
    private Integer standardWorkingDays;

    @Column(name = "standard_hours_per_day")
    private Integer standardHoursPerDay;

    @Column(name = "overtime_normal_rate", precision = 5, scale = 2)
    private BigDecimal overtimeNormalRate;

    @Column(name = "overtime_weekend_rate", precision = 5, scale = 2)
    private BigDecimal overtimeWeekendRate;

    @Column(name = "overtime_holiday_rate", precision = 5, scale = 2)
    private BigDecimal overtimeHolidayRate;

    @Column(name = "overtime_night_rate", precision = 5, scale = 2)
    private BigDecimal overtimeNightRate;

    @Column(name = "late_penalty_per_minute", precision = 15, scale = 2)
    private BigDecimal latePenaltyPerMinute;

    @Column(name = "union_due_rate", precision = 5, scale = 4)
    private BigDecimal unionDueRate;

    @Column(name = "max_union_due", precision = 15, scale = 2)
    private BigDecimal maxUnionDue;

    @Column(name = "is_active")
    private Boolean isActive;
}
