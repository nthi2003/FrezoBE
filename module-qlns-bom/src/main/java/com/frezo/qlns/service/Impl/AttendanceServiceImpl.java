package com.frezo.qlns.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.common.exception.AppException;
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
import com.frezo.qtht.dto.response.GeoAttendanceConfig;
import com.frezo.qtht.dto.response.SystemDetailsSettingResponse;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.entity.Setting;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Slf4j

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceMapper attendanceMapper;
    private final SettingRepository settingRepository;
    private final PersonRepository personRepository;
    private final ObjectMapper objectMapper;

    // ---- Đọc config thời gian từ Setting ----
    // Cho phép mỗi tổ chức tự đặt giờ bắt đầu/kết thúc ca sáng, ca chiều
    // Nếu chưa config thì dùng mặc định: sáng 8:00-12:00, chiều 13:00-17:30
    private String getSettingVal(String orgId, java.util.function.Function<Setting, String> getter) {
        Setting s = getSetting(orgId);
        return s != null ? getter.apply(s) : null;
    }

    private LocalTime getMorningStart(String orgId) { return parseTime(getSettingVal(orgId, Setting::getMorningStart), LocalTime.of(8, 0)); }
    private LocalTime getMorningEnd(String orgId) { return parseTime(getSettingVal(orgId, Setting::getMorningEnd), LocalTime.of(12, 0)); }
    private LocalTime getAfternoonStart(String orgId) { return parseTime(getSettingVal(orgId, Setting::getAfternoonStart), LocalTime.of(13, 0)); }
    private LocalTime getAfternoonEnd(String orgId) { return parseTime(getSettingVal(orgId, Setting::getAfternoonEnd), LocalTime.of(17, 30)); }

    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private LocalTime parseTime(String val, LocalTime fallback) {
        if (val == null || val.isBlank()) return fallback;
        try { return LocalTime.parse(val.trim(), TIME_FMT); }
        catch (Exception e) { return fallback; }
    }

    private Setting getSetting(String orgId) {
        return settingRepository.findByOrgIdAndIsDeletedFalse(orgId)
                .orElse(null);
    }

    // ---- Lấy orgId từ personId ----
    // Dùng để tra cứu config của tổ chức mà nhân viên đó thuộc về
    private String resolveOrgId(String personId) {
        if (personId == null) return null;
        return personRepository.findById(personId)
                .map(Person::getOrgId)
                .orElse(null);
    }

    // ---- Đọc config GPS/WiFi từ Setting.details (JSON) ----
    // Cấu hình này chứa: officeLat, officeLng, bán kính cho phép, danh sách WiFi hợp lệ
    private GeoAttendanceConfig getGeoConfig(String orgId) {
        Setting setting = settingRepository.findByOrgIdAndIsDeletedFalse(orgId).orElse(null);
        if (setting == null || setting.getDetails() == null || setting.getDetails().isBlank()) return null;
        try {
            SystemDetailsSettingResponse details = objectMapper.readValue(setting.getDetails(), SystemDetailsSettingResponse.class);
            return details.getGeo();
        } catch (Exception e) {
            log.warn("Failed to parse geo config for org {}: {}", orgId, e.getMessage());
            return null;
        }
    }

    // ---- Kiểm tra toạ độ GPS có nằm trong bán kính cho phép không ----
    // Dùng công thức Haversine để tính khoảng cách từ vị trí nhân viên đến văn phòng
    private boolean isLocationValid(GeoAttendanceConfig geo, Double lat, Double lng) {
        if (geo == null || lat == null || lng == null) return true;
        if (geo.getOfficeLatitude() == null || geo.getOfficeLongitude() == null) return true;
        double dist = haversine(lat, lng, geo.getOfficeLatitude(), geo.getOfficeLongitude());
        int allowed = geo.getAllowedRadiusMeters() != null ? geo.getAllowedRadiusMeters() : 300;
        return dist <= allowed;
    }

    // ---- Kiểm tra WiFi có nằm trong danh sách cho phép không ----
    // So khớp SSID hoặc BSSID với danh sách cấu hình (phân cách bằng dấu phẩy)
    private boolean isWifiValid(GeoAttendanceConfig geo, String ssid, String bssid) {
        if (geo == null) return true;
        if (geo.getAllowedWifiSsids() != null && !geo.getAllowedWifiSsids().isBlank() && ssid != null) {
            String[] allowed = geo.getAllowedWifiSsids().split(",");
            for (String a : allowed) {
                if (a.trim().equalsIgnoreCase(ssid.trim())) return true;
            }
            return false;
        }
        if (geo.getAllowedWifiBssids() != null && !geo.getAllowedWifiBssids().isBlank() && bssid != null) {
            String[] allowed = geo.getAllowedWifiBssids().split(",");
            for (String a : allowed) {
                if (a.trim().equalsIgnoreCase(bssid.trim())) return true;
            }
            return false;
        }
        return true;
    }

    // ---- Công thức Haversine tính khoảng cách giữa 2 toạ độ GPS (mét) ----
    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ---- CHECK-IN: Nhân viên check-in từ Mobile App ----
    // 1. Kiểm tra bản ghi chấm công đã tồn tại chưa (theo personId + ngày)
    // 2. Validate vị trí GPS (trong bán kính cho phép) và WiFi (trong danh sách)
    // 3. Lưu thông tin check-in kèm GPS/WiFi metadata
    // 4. Tính số phút đi muộn dựa trên shiftType và config giờ làm việc
    @Override
    public AttendanceResponse checkIn(AttendanceCheckInRequest request) {
        Optional<Attendance> existing = attendanceRepository.findByPersonIdAndAttendanceDate(
                request.getPersonId(), request.getAttendanceDate());

        String orgId = resolveOrgId(request.getPersonId());
        GeoAttendanceConfig geo = getGeoConfig(orgId);
        if (!isLocationValid(geo, request.getLatitude(), request.getLongitude())) {
            throw new AppException("attendance.location.outside", HttpStatus.BAD_REQUEST);
        }
        if (!isWifiValid(geo, request.getWifiSsid(), request.getWifiBssid())) {
            throw new AppException("attendance.wifi.not.allowed", HttpStatus.BAD_REQUEST);
        }

        Attendance attendance;
        if (existing.isPresent()) {
            attendance = existing.get();
            attendance.setCheckInTime(request.getCheckInTime());
            attendance.setCheckInLatitude(request.getLatitude());
            attendance.setCheckInLongitude(request.getLongitude());
            attendance.setCheckInWifiSsid(request.getWifiSsid());
            attendance.setCheckInWifiBssid(request.getWifiBssid());
        } else {
            attendance = attendanceMapper.toEntity(request);
        }
        LocalTime morningStart = getMorningStart(orgId);
        LocalTime morningEnd = getMorningEnd(orgId);
        LocalTime afternoonStart = getAfternoonStart(orgId);
        LocalTime afternoonEnd = getAfternoonEnd(orgId);

        int lateMinutes = 0;
        if ("MORNING".equals(request.getShiftType()) || "FULL".equals(request.getShiftType())) {
             if (request.getCheckInTime().isAfter(morningStart)) {
                 lateMinutes = (int) ChronoUnit.MINUTES.between(morningStart, request.getCheckInTime());
             }
        } else if ("AFTERNOON".equals(request.getShiftType())) {
            if (request.getCheckInTime().isAfter(afternoonStart)) {
                 lateMinutes = (int) ChronoUnit.MINUTES.between(afternoonStart, request.getCheckInTime());
             }
        }
        attendance.setLateMinutes(Math.max(lateMinutes, 0));
        attendance.setStatus(AttendanceStatus.PRESENT);
        
        Attendance saved = attendanceRepository.save(attendance);
        return attendanceMapper.toResponse(saved);
    }

    // ---- CHECK-OUT: Nhân viên check-out từ Mobile App ----
    // 1. Phải check-in trước mới được check-out
    // 2. Validate vị trí GPS/WiFi
    // 3. Lưu thông tin check-out kèm metadata
    // 4. Tính workMinutes (tổng phút làm việc), trừ 60p nghỉ trưa nếu là FULL
    // 5. Tính overtimeMinutes (làm sau giờ kết thúc ca)
    @Override
    public AttendanceResponse checkOut(AttendanceCheckOutRequest request) {
        // ThiNVQ : Phải check-in trước mới được check-out
        Attendance attendance = attendanceRepository.findByPersonIdAndAttendanceDate(
                request.getPersonId(), request.getAttendanceDate())
                .orElseThrow(() -> new QTHTException("error.attendance.not.checked.in"));

        String orgId = resolveOrgId(request.getPersonId());
        GeoAttendanceConfig geo = getGeoConfig(orgId);
        if (!isLocationValid(geo, request.getLatitude(), request.getLongitude())) {
            throw new AppException("attendance.location.outside", HttpStatus.BAD_REQUEST);
        }
        if (!isWifiValid(geo, request.getWifiSsid(), request.getWifiBssid())) {
            throw new AppException("attendance.wifi.not.allowed", HttpStatus.BAD_REQUEST);
        }

        attendance.setCheckOutTime(request.getCheckOutTime());
        attendance.setCheckOutLatitude(request.getLatitude());
        attendance.setCheckOutLongitude(request.getLongitude());
        attendance.setCheckOutWifiSsid(request.getWifiSsid());
        attendance.setCheckOutWifiBssid(request.getWifiBssid());
        int workMinutes = 0;
        int overtimeMinutes = 0;
        
        if (attendance.getCheckInTime() != null) {
            LocalTime morningEnd = getMorningEnd(orgId);
            LocalTime afternoonStart = getAfternoonStart(orgId);
            LocalTime afternoonEnd = getAfternoonEnd(orgId);

            workMinutes = (int) ChronoUnit.MINUTES.between(attendance.getCheckInTime(), request.getCheckOutTime());
            if ("FULL".equals(attendance.getShiftType()) && 
                attendance.getCheckInTime().isBefore(morningEnd) && 
                request.getCheckOutTime().isAfter(afternoonStart)) {
                workMinutes -= 60;
            }
            
            if (request.getCheckOutTime().isAfter(afternoonEnd)) {
                overtimeMinutes = (int) ChronoUnit.MINUTES.between(afternoonEnd, request.getCheckOutTime());
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
