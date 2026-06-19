package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "employee_dependent")
public class EmployeeDependent extends BaseEntity {

    @Column(name = "person_id", nullable = false)
    private String personId;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Column(name = "relationship", length = 50)
    private String relationship;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "tax_code", length = 20)
    private String taxCode;

    @Column(name = "from_month")
    private Integer fromMonth;

    @Column(name = "from_year")
    private Integer fromYear;

    @Column(name = "to_month")
    private Integer toMonth;

    @Column(name = "to_year")
    private Integer toYear;

    @Column(name = "is_active")
    private Boolean isActive;
}
