package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "leave_record")
public class LeaveRecord extends BaseEntity {

    @Column(name = "person_id", nullable = false)
    private String personId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "leave_type", length = 50)
    private String leaveType; // ANNUAL, SICK, UNPAID, MATERNITY

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", length = 50)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "approved_by")
    private String approvedBy;
}
