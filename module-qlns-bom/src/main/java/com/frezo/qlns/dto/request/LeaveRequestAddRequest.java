package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestAddRequest {
    private String contractId;
    private String personId;
    private String leaveType; // annual / sick / unpaid / other
    private LocalDate startDate;
    private LocalDate endDate;
    private Double durationDays;
    private String reason;
}
