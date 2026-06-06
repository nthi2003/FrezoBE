package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.PageResponse;
import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.dto.request.AttendanceCheckInRequest;
import com.frezo.qlns.dto.request.AttendanceCheckOutRequest;
import com.frezo.qlns.dto.request.AttendanceFilter;
import com.frezo.qlns.dto.response.AttendanceResponse;
import com.frezo.qlns.entity.Attendance;
import com.frezo.qlns.mapper.AttendanceMapper;
import com.frezo.qlns.repository.AttendanceRepository;
import com.frezo.qlns.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;

    private static final LocalTime MORNING_START = LocalTime.of(8, 0);
    private static final LocalTime MORNING_END = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.of(13, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(17, 30);

    @Override
    public AttendanceResponse checkIn(AttendanceCheckInRequest request) {
        Optional<Attendance> existing = attendanceRepository.findByPersonIdAndAttendanceDate(
                request.getPersonId(), request.getAttendanceDate());

        Attendance attendance;
        if (existing.isPresent()) {
            attendance = existing.get();
            attendance.setCheckInTime(request.getCheckInTime());
        } else {
            attendance = attendanceMapper.toEntity(request);
        }

        // Tính phút đi muộn (giả sử ca sáng bắt đầu lúc 8h)
        int lateMinutes = 0;
        if ("MORNING".equals(request.getShiftType()) || "FULL".equals(request.getShiftType())) {
             if (request.getCheckInTime().isAfter(MORNING_START)) {
                 lateMinutes = (int) ChronoUnit.MINUTES.between(MORNING_START, request.getCheckInTime());
             }
        } else if ("AFTERNOON".equals(request.getShiftType())) {
            if (request.getCheckInTime().isAfter(AFTERNOON_START)) {
                 lateMinutes = (int) ChronoUnit.MINUTES.between(AFTERNOON_START, request.getCheckInTime());
             }
        }
        attendance.setLateMinutes(Math.max(lateMinutes, 0));
        attendance.setStatus(AttendanceStatus.PRESENT);
        
        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toResponse(saved);
    }

    @Override
    public AttendanceResponse checkOut(AttendanceCheckOutRequest request) {
        // ThiNVQ : Phải check-in trước mới được check-out
        Attendance attendance = attendanceRepository.findByPersonIdAndAttendanceDate(
                request.getPersonId(), request.getAttendanceDate())
                .orElseThrow(() -> new QTHTException("error.attendance.not.checked.in"));

        attendance.setCheckOutTime(request.getCheckOutTime());

        // Tính số phút làm việc
        int workMinutes = 0;
        int overtimeMinutes = 0;
        
        if (attendance.getCheckInTime() != null) {
            workMinutes = (int) ChronoUnit.MINUTES.between(attendance.getCheckInTime(), request.getCheckOutTime());
            // Trừ đi 1 tiếng nghỉ trưa nếu làm cả ngày
            if ("FULL".equals(attendance.getShiftType()) && 
                attendance.getCheckInTime().isBefore(MORNING_END) && 
                request.getCheckOutTime().isAfter(AFTERNOON_START)) {
                workMinutes -= 60;
            }
            
            // Tính overtime (nếu làm sau 17:30)
            if (request.getCheckOutTime().isAfter(AFTERNOON_END)) {
                overtimeMinutes = (int) ChronoUnit.MINUTES.between(AFTERNOON_END, request.getCheckOutTime());
            }
        }

        attendance.setWorkMinutes(Math.max(workMinutes, 0));
        attendance.setOvertimeMinutes(Math.max(overtimeMinutes, 0));

        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toResponse(saved);
    }

    @Override
    public PageResponse<AttendanceResponse> all(AttendanceFilter filter) {
        Specification<Attendance> spec = Specification.where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));

        if (SystemUtils.isNotNullOrEmpty(filter.getPersonId())) {
            spec = spec.and(GenericSpecification.equalField("personId", filter.getPersonId()));
        }

        // ThiNVQ : Filter theo tháng/năm nếu có truyền vào
        if (filter.getMonth() != null && filter.getYear() != null) {
            spec = spec.and(GenericSpecification.monthOfDateField("attendanceDate", filter.getMonth(), filter.getYear()));
        }

        if (filter.getDate() != null) {
            spec = spec.and(GenericSpecification.equalField("attendanceDate", filter.getDate()));
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "attendanceDate");
        Page<Attendance> page = attendanceRepository.findAll(spec, ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));

        List<AttendanceResponse> responses = page.getContent().stream().map(attendanceMapper::toResponse).toList();
        int pageNum = filter.getPageNumber() != null ? filter.getPageNumber() : 1;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 10;
        return PageResponse.of(pageNum, pageSize, page, responses);
    }

    @Override
    public AttendanceResponse getById(String id) {
        return attendanceRepository.findById(id).map(attendanceMapper::toResponse)
                .orElseThrow(() -> new QTHTException("error.attendance.not.found"));
    }

    @Override
    public void approve(String id, String approvedBy) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.attendance.not.found"));
        attendance.setApprovedBy(approvedBy);
        attendanceRepository.save(attendance);
    }
}
