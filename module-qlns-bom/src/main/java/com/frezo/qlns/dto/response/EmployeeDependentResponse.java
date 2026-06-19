package com.frezo.qlns.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmployeeDependentResponse {
    private String id;
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
