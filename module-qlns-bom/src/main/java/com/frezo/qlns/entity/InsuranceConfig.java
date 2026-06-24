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
@Table(name = "insurance_config")
public class InsuranceConfig extends BaseEntity {

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "social_insurance_rate", precision = 5, scale = 4)
    private BigDecimal socialInsuranceRate;

    @Column(name = "health_insurance_rate", precision = 5, scale = 4)
    private BigDecimal healthInsuranceRate;

    @Column(name = "unemployment_insurance_rate", precision = 5, scale = 4)
    private BigDecimal unemploymentInsuranceRate;

    @Column(name = "employer_social_rate", precision = 5, scale = 4)
    private BigDecimal employerSocialRate;

    @Column(name = "employer_health_rate", precision = 5, scale = 4)
    private BigDecimal employerHealthRate;

    @Column(name = "employer_unemployment_rate", precision = 5, scale = 4)
    private BigDecimal employerUnemploymentRate;

    @Column(name = "employer_accident_rate", precision = 5, scale = 4)
    private BigDecimal employerAccidentRate;

    @Column(name = "max_insurance_salary", precision = 15, scale = 2)
    private BigDecimal maxInsuranceSalary;

    @Column(name = "base_minimum_wage", precision = 15, scale = 2)
    private BigDecimal baseMinimumWage;

    @Column(name = "regional_minimum_wage", precision = 15, scale = 2)
    private BigDecimal regionalMinimumWage;

    @Column(name = "region", length = 10)
    private String region;

    @Column(name = "applies_from")
    private Integer appliesFrom;

    @Column(name = "applies_to")
    private Integer appliesTo;

    @Column(name = "is_active")
    private Boolean isActive;
}
