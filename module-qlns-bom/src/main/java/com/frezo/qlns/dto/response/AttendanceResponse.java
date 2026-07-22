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
    /** Enriched for admin roster / daily list */
    private String personName;
    private String departmentId;
    private String departmentName;
    /** OK | LATE | NOT_CHECKED_IN | CHECKED_OUT | ABSENT | … */
    private String displayStatus;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private Double checkInLatitude;
    private Double checkInLongitude;
    private String checkInWifiSsid;
    private String checkInWifiBssid;
    private LocalTime checkOutTime;
    private Double checkOutLatitude;
    private Double checkOutLongitude;
    private String checkOutWifiSsid;
    private String checkOutWifiBssid;
    private Integer workMinutes;
    private Integer lateMinutes;
    private Integer overtimeMinutes;
    private String shiftType;
    private AttendanceStatus status;
    private String approvedBy;
    private String note;
}
