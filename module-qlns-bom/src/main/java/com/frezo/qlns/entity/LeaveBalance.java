package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "leave_balance", uniqueConstraints =
    @UniqueConstraint(columnNames = {"person_id", "year"}))
public class LeaveBalance extends BaseEntity {

    @Column(name = "person_id", nullable = false, length = 36)
    private String personId;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "total_days", nullable = false)
    private Double totalDays;

    @Column(name = "used_days", nullable = false)
    private Double usedDays;

    @Column(name = "last_reset_at")
    private LocalDate lastResetAt;
}
