package com.frezo.qlns.service;

import com.frezo.common.response.PageResponse;
import com.frezo.qlns.dto.request.AttendanceCheckInRequest;
import com.frezo.qlns.dto.request.AttendanceCheckOutRequest;
import com.frezo.qlns.dto.request.AttendanceFilter;
import com.frezo.qlns.dto.response.AttendanceResponse;
import com.frezo.qlns.dto.response.AttendanceStatsResponse;
import com.frezo.qlns.dto.response.MyAttendanceDayResponse;
import com.frezo.qlns.dto.response.MyAttendanceMonthResponse;

public interface AttendanceService {
    AttendanceResponse checkIn(AttendanceCheckInRequest request);
    AttendanceResponse checkOut(AttendanceCheckOutRequest request);
    PageResponse<AttendanceResponse> all(AttendanceFilter filter);

    /**
     * Company-wide daily roster: mọi NV active + trạng thái chấm công trong ngày
     * (kể cả chưa check-in).
     */
    PageResponse<AttendanceResponse> daily(AttendanceFilter filter);

    AttendanceResponse getById(String id);
    void approve(String id, String approvedBy);

    /**
     * KPI tổng hợp 1 tháng cho Home dashboard Mobile.
     * Nếu personId null → fallback current user's personId.
     */
    AttendanceStatsResponse getStats(String personId, String contractId, Integer month, Integer year);

    /**
     * Lịch làm + chấm công tháng của user đăng nhập (principal → personId).
     * Không nhận personId từ client.
     */
    MyAttendanceMonthResponse getMyMonth(Integer month, Integer year);

    /** Ngày hôm nay của user đăng nhập (cùng shape một phần tử trong {@link #getMyMonth}). */
    MyAttendanceDayResponse getMyToday();
}
