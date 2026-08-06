package com.frezo.qlns.entity;

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

import java.time.LocalDate;

/** Quá trình làm việc của nhân viên. */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hr_person_work_history", indexes = {
        @Index(name = "idx_hr_work_hist_person", columnList = "person_id")
})
public class PersonWorkHistory extends BaseEntity {

    @Column(name = "person_id", length = 36, nullable = false)
    private String personId;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "to_date")
    private LocalDate toDate;

    @Column(name = "department_name", length = 255)
    private String departmentName;

    @Column(name = "position_name", length = 255)
    private String positionName;

    @Column(name = "job_position_id", length = 36)
    private String jobPositionId;

    @Column(name = "note", length = 2000)
    private String note;
}
