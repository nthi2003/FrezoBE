package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PersonWorkHistoryResponse {
    private String id;
    private String personId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String departmentName;
    private String positionName;
    private String jobPositionId;
    private String note;
}
