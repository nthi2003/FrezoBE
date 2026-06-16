package com.frezo.qlns.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceCheckOutRequest {
    private String personId;
    private LocalDate attendanceDate;
    private LocalTime checkOutTime;
    private Double latitude;
    private Double longitude;
    private String wifiSsid;
    private String wifiBssid;
}
