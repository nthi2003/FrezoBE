package com.frezo.qlns.dto.response;

import com.frezo.qlns.common.AttendanceStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceResponse {
    private String id;
    private String contractId;
    private String personId;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private Integer workMinutes;
    private Integer lateMinutes;
    private Integer overtimeMinutes;
    private String shiftType;
    private AttendanceStatus status;
    private String approvedBy;
    private String note;
}
