package com.frezo.qlns.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestResponse {
    private String id;
    private String contractId;
    private String personId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double durationDays;
    private String reason;
    private String status;
    private String approvedBy;
    private LocalDate approvedAt;
    private String rejectedBy;
    private String rejectReason;
    private String createdBy;
    private String createdDate;
}
