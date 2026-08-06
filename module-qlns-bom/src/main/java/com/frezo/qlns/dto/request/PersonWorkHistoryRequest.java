package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonWorkHistoryRequest {
    private String personId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String departmentName;
    private String positionName;
    private String jobPositionId;
    private String note;
}
