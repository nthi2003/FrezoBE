package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPI tổng hợp cho Home dashboard Mobile — 1 tháng của 1 nhân viên.
 * Endpoint: {@code GET /qlns/attendance/stats?personId=&month=&year=}.
 *
 * Ý nghĩa các field:
 * - workingDays   : Số ngày làm việc chuẩn của tháng (thứ 2-6, không tính T7/CN).
 * - presentDays   : Số ngày có bản ghi chấm công status=PRESENT.
 * - lateDays      : Số ngày lateMinutes > 0.
 * - absentDays    : Số ngày status=ABSENT.
 * - leaveDaysApproved : Tổng ngày phép đã duyệt trong tháng (approve = APPROVED).
 * - leaveBalance  : Số ngày phép năm còn lại (12 - đã dùng năm nay).
 * - totalLateMinutes / totalOvertimeMinutes : cộng dồn cả tháng.
 * - totalWorkMinutes : cộng dồn workMinutes cả tháng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatsResponse {
    private Integer month;
    private Integer year;

    private Integer workingDays;
    private Integer presentDays;
    private Integer lateDays;
    private Integer absentDays;

    private Integer totalWorkMinutes;
    private Integer totalLateMinutes;
    private Integer totalOvertimeMinutes;

    private Double leaveDaysApproved;
    private Double leaveBalance;

    /** Ngày cuối cùng có bản ghi (để FE hiện "cập nhật lần cuối ..."). */
    private String lastAttendanceDate;
}
