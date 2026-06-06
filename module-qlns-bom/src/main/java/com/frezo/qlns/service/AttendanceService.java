package com.frezo.qlns.service;

import com.frezo.common.response.PageResponse;
import com.frezo.qlns.dto.request.AttendanceCheckInRequest;
import com.frezo.qlns.dto.request.AttendanceCheckOutRequest;
import com.frezo.qlns.dto.request.AttendanceFilter;
import com.frezo.qlns.dto.response.AttendanceResponse;

public interface AttendanceService {
    AttendanceResponse checkIn(AttendanceCheckInRequest request);
    AttendanceResponse checkOut(AttendanceCheckOutRequest request);
    PageResponse<AttendanceResponse> all(AttendanceFilter filter);
    AttendanceResponse getById(String id);
    void approve(String id, String approvedBy);
}
