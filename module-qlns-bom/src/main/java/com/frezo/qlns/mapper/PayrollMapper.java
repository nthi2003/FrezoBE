package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qlns.entity.Payroll;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring")
public interface PayrollMapper {

    @Mapping(target = "baseSalary", source = "basicSalary")
    @Mapping(target = "payMonth", source = "month")
    @Mapping(target = "payYear", source = "year")
    @Mapping(target = "standardWorkingDays", source = "standardDays")
    @Mapping(target = "workingDaysAmount", ignore = true)
    @Mapping(target = "overtimeHours", ignore = true)
    @Mapping(target = "overtimeAmount", source = "overtimePay")
    @Mapping(target = "bonusAmount", source = "bonus")
    @Mapping(target = "bonusReason", source = "note")
    @Mapping(target = "bhxh", source = "socialInsurance")
    @Mapping(target = "bhyt", source = "healthInsurance")
    @Mapping(target = "bhtn", source = "unemploymentInsurance")
    @Mapping(target = "tax", source = "taxIncome")
    @Mapping(target = "otherDeduction", source = "otherDeductions")
    @Mapping(target = "totalDeduction", source = "totalDeductions")
    @Mapping(target = "totalNet", source = "netSalary")
    @Mapping(target = "paidDate", source = "paidAt")
    @Mapping(target = "createdDate", source = "createdDate")
    @Mapping(target = "updatedDate", source = "updatedDate")
    @Mapping(target = "period", ignore = true)
    @Mapping(target = "statusCode", ignore = true)
    @Mapping(target = "statusLabel", expression = "java(getStatusLabel(entity.getStatus()))")
    // Fields sẽ được enrich sau ở service — mapper mặc định null/ignore
    @Mapping(target = "personName", ignore = true)
    @Mapping(target = "personCode", ignore = true)
    @Mapping(target = "orgName", ignore = true)
    @Mapping(target = "dependentCount", ignore = true)
    @Mapping(target = "personalDeduction", ignore = true)
    @Mapping(target = "dependentDeduction", ignore = true)
    @Mapping(target = "taxBrackets", ignore = true)
    @Mapping(target = "socialInsuranceRate", ignore = true)
    @Mapping(target = "healthInsuranceRate", ignore = true)
    @Mapping(target = "unemploymentInsuranceRate", ignore = true)
    @Mapping(target = "maxInsuranceSalary", ignore = true)
    @Mapping(target = "employerSocialInsurance", ignore = true)
    @Mapping(target = "employerHealthInsurance", ignore = true)
    @Mapping(target = "employerUnemploymentInsurance", ignore = true)
    @Mapping(target = "employerAccidentInsurance", ignore = true)
    @Mapping(target = "employerSocialRate", ignore = true)
    @Mapping(target = "employerHealthRate", ignore = true)
    @Mapping(target = "employerUnemploymentRate", ignore = true)
    @Mapping(target = "employerAccidentRate", ignore = true)
    @Mapping(target = "totalEmployerContribution", ignore = true)
    @Mapping(target = "totalCompanyCost", ignore = true)
    @Mapping(target = "region", ignore = true)
    @Mapping(target = "regionalMinimumWage", ignore = true)
    @Mapping(target = "minimumWageCompliant", ignore = true)
    PayrollResponse toResponse(Payroll entity);

    /** Sau khi map cơ bản, tính các derived field không cần external data. */
    @AfterMapping
    default void enrichBasic(Payroll entity, @MappingTarget PayrollResponse res) {
        // period: "MM/yyyy"
        if (entity.getMonth() != null && entity.getYear() != null) {
            res.setPeriod(String.format("%02d/%d", entity.getMonth(), entity.getYear()));
        }

        // statusCode string
        res.setStatusCode(getStatusCode(entity.getStatus()));

        // Tổng OT hours
        BigDecimal totalOt = BigDecimal.ZERO;
        if (entity.getOvertimeHoursNormal() != null)  totalOt = totalOt.add(entity.getOvertimeHoursNormal());
        if (entity.getOvertimeHoursWeekend() != null) totalOt = totalOt.add(entity.getOvertimeHoursWeekend());
        if (entity.getOvertimeHoursHoliday() != null) totalOt = totalOt.add(entity.getOvertimeHoursHoliday());
        res.setOvertimeHours(totalOt);

        // workingDaysAmount = salaryPerDay × actualDays
        if (entity.getBasicSalary() != null && entity.getStandardDays() != null
                && entity.getStandardDays() > 0 && entity.getActualWorkingDays() != null) {
            BigDecimal perDay = entity.getBasicSalary()
                    .divide(BigDecimal.valueOf(entity.getStandardDays()), 10, RoundingMode.HALF_UP);
            res.setWorkingDaysAmount(perDay.multiply(entity.getActualWorkingDays())
                    .setScale(0, RoundingMode.HALF_UP));
        }
    }

    default String getStatusLabel(Integer status) {
        if (status == null) return "Bản nháp";
        return switch (status) {
            case 0 -> "Bản nháp";
            case 1 -> "Đã xác nhận";
            case 2 -> "Đã thanh toán";
            default -> "Không xác định";
        };
    }

    default String getStatusCode(Integer status) {
        if (status == null) return "DRAFT";
        return switch (status) {
            case 0 -> "DRAFT";
            case 1 -> "CONFIRMED";
            case 2 -> "PAID";
            default -> "DRAFT";
        };
    }
}
