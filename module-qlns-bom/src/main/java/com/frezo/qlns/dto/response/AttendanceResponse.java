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
