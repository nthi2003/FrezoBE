package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Một ngày trong lịch tự chấm công (mobile/web friendly).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAttendanceDayResponse {
    /** YYYY-MM-DD */
    private String date;
    /** 1=Mon … 7=Sun (ISO) */
    private Integer dayOfWeek;
    private Boolean workDay;
    private Boolean weekend;
    private Boolean holiday;
    private Boolean onLeave;
    private String leaveType;
    private String leaveLabel;
    private MyAttendanceMonthResponse.ShiftInfo shift;
    private DayAttendance attendance;
    private DayActions actions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayAttendance {
        private String id;
        private String checkIn;
        private String checkOut;
        private String status;
        private String displayStatus;
        private Integer lateMinutes;
        private Integer workMinutes;
        private Integer overtimeMinutes;
        private String shiftType;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayActions {
        private Boolean canCheckIn;
        private Boolean canCheckOut;
    }
}
