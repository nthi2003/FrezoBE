package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeDependentRequest {
    private String personId;
    private String fullName;
    private String relationship;
    private LocalDate birthDate;
    private String taxCode;
    private Integer fromMonth;
    private Integer fromYear;
    private Integer toMonth;
    private Integer toYear;
    private Boolean isActive;
}
