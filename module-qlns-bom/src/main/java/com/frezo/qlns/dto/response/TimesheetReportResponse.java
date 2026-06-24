package com.frezo.qlns.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class TimesheetReportResponse {
    private String personId;
    private String personCode;
    private String personName;
    private int month;
    private int year;
    private int totalDays;
    private int presentDays;
    private int lateDays;
    private int absentDays;
    private int leaveDays;
    private int halfDays;
    private int holidayDays;
    private int totalLateMinutes;
    private int totalOvertimeMinutes;
    private List<TimesheetDayResponse> details;

    @Data
    public static class TimesheetDayResponse {
        private int day;
        private String status;
        private String checkIn;
        private String checkOut;
        private Integer workMinutes;
        private Integer lateMinutes;
    }
}
