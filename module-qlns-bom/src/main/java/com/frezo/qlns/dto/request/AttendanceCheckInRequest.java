package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceCheckInRequest {
    private String contractId;
    private String personId;
    private LocalDate attendanceDate;
    private LocalTime checkInTime;
    private String shiftType; // MORNING / AFTERNOON / FULL / NIGHT
    private Double latitude;
    private Double longitude;
    private String wifiSsid;
    private String wifiBssid;
    private String note;
}
