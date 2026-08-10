package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Self-service lịch làm + chấm công theo tháng của user đang đăng nhập.
 * Endpoint: {@code GET /qlns/attendance/me/month?month=&year=}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyAttendanceMonthResponse {
    private PersonSummary person;
    private Integer month;
    private Integer year;
    private Integer daysInMonth;
    private MyAttendanceDayResponse today;
    private List<MyAttendanceDayResponse> days;
    private ShiftInfo defaultShift;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonSummary {
        private String personId;
        private String personName;
        private String personCode;
        private String departmentId;
        private String departmentName;
        private String orgId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftInfo {
        private String start;
        private String end;
        private String label;
    }
}
