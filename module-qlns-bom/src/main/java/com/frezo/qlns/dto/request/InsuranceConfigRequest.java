package com.frezo.qlns.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InsuranceConfigRequest {
    private Integer year;
    private BigDecimal socialInsuranceRate;
    private BigDecimal healthInsuranceRate;
    private BigDecimal unemploymentInsuranceRate;
    private BigDecimal employerSocialRate;
    private BigDecimal employerHealthRate;
    private BigDecimal employerUnemploymentRate;
    private BigDecimal employerAccidentRate;
    private BigDecimal maxInsuranceSalary;
    private BigDecimal baseMinimumWage;
    private BigDecimal regionalMinimumWage;
    private String region;
    private Integer appliesFrom;
    private Integer appliesTo;
    private Boolean isActive;
}
