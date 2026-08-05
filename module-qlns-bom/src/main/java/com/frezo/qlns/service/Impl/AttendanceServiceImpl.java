package com.frezo.qlns.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.common.exception.AppException;
import com.frezo.qlns.common.QlnsErrorCode;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.PageResponse;
import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.dto.request.AttendanceCheckInRequest;
import com.frezo.qlns.dto.request.AttendanceCheckOutRequest;
import com.frezo.qlns.dto.request.AttendanceFilter;
import com.frezo.qlns.dto.response.AttendanceResponse;
import com.frezo.qlns.dto.response.AttendanceStatsResponse;
import com.frezo.qlns.entity.Attendance;
import com.frezo.qlns.mapper.AttendanceMapper;
import com.frezo.qlns.repository.AttendanceRepository;
import com.frezo.qlns.repository.LeaveRequestRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final LeaveRequestRepository leaveRequestRepository;

    // Số ngày phép năm mặc định — TODO: sau này đọc từ Contract/HR policy.
    private static final double DEFAULT_ANNUAL_LEAVE_DAYS = 12.0;

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

    public static final String POPUP_ATTENDANCE_FIRST_CHECKIN = "ATTENDANCE_FIRST_CHECKIN";

    // ---- CHECK-IN: Nhân viên check-in từ Mobile App ----
    // 1. Kiểm tra bản ghi chấm công đã tồn tại chưa (theo personId + ngày)
    // 2. IDEMPOTENT: nếu đã có checkInTime hôm nay → return existing (retry offline queue an toàn)
    // 3. Validate vị trí GPS (trong bán kính cho phép) và WiFi (trong danh sách)
    // 4. Lưu thông tin check-in kèm GPS/WiFi metadata
    // 5. Tính số phút đi muộn dựa trên shiftType và config giờ làm việc
    // 6. First punch of day → popupEvent = ATTENDANCE_FIRST_CHECKIN (FE load template)
    @Override
    public AttendanceResponse checkIn(AttendanceCheckInRequest request) {
        Optional<Attendance> existing = attendanceRepository.findByPersonIdAndAttendanceDate(
                request.getPersonId(), request.getAttendanceDate());

        // Idempotent guard: đã check-in rồi thì trả lại record cũ, không ghi đè.
        // Cần thiết để offline queue retry / mạng chập chờn không tạo lịch sử sai.
        if (existing.isPresent() && existing.get().getCheckInTime() != null) {
            return attendanceMapper.toResponse(existing.get());
        }

        // True first punch hôm nay (chưa có checkInTime) → UX popup
        boolean firstCheckInOfDay = true;

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
        int late = Math.max(lateMinutes, 0);
        attendance.setLateMinutes(late);
        attendance.setStatus(late > 0 ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);

        Attendance saved = attendanceRepository.save(attendance);
        // Enrich displayStatus (LATE/OK) — FE/mobile không phải đoán từ lateMinutes
        AttendanceResponse response = enrichResponse(attendanceMapper.toResponse(saved), null);
        if (firstCheckInOfDay) {
            response.setPopupEvent(POPUP_ATTENDANCE_FIRST_CHECKIN);
        }
        return response;
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
                .orElseThrow(() -> new AppException(QlnsErrorCode.ATTENDANCE_NOT_CHECKED_IN));

        // Idempotent guard: đã check-out rồi thì trả lại record cũ, không ghi đè giờ.
        if (attendance.getCheckOutTime() != null) {
            return attendanceMapper.toResponse(attendance);
        }

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
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> all(AttendanceFilter filter) {
        Specification<Attendance> spec = Specification.where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));

        if (SystemUtils.isNotNullOrEmpty(filter.getPersonId())) {
            spec = spec.and(GenericSpecification.equalField("personId", filter.getPersonId()));
        }
        if (SystemUtils.isNotNullOrEmpty(filter.getContractId())) {
            spec = spec.and(GenericSpecification.equalField("contractId", filter.getContractId()));
        }
        if (SystemUtils.isNotNullOrEmpty(filter.getStatus())) {
            AttendanceStatus status = parseAttendanceStatus(filter.getStatus());
            if (status != null) {
                spec = spec.and(GenericSpecification.equalField("status", status));
            }
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

        List<AttendanceResponse> responses = page.getContent().stream()
                .map(a -> enrichResponse(attendanceMapper.toResponse(a), null))
                .toList();

        if (SystemUtils.isNotNullOrEmpty(filter.getDepartmentId())) {
            String deptId = filter.getDepartmentId();
            responses = responses.stream()
                    .filter(r -> deptId.equals(r.getDepartmentId()))
                    .toList();
        }

        int pageNum = filter.getPageNumber() != null ? filter.getPageNumber() : 1;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 10;
        return PageResponse.of(pageNum, pageSize, page, responses);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> daily(AttendanceFilter filter) {
        if (filter.getDate() == null) {
            throw new AppException("attendance.daily.date.required", HttpStatus.BAD_REQUEST);
        }

        List<Person> persons = personRepository.findActiveWithDepartment(
                filter.getDepartmentId(), filter.getPersonId());

        List<Attendance> dayRecords = attendanceRepository.findByAttendanceDateAndIsDeletedFalse(filter.getDate());
        Map<String, Attendance> byPerson = new HashMap<>();
        for (Attendance a : dayRecords) {
            if (a.getPersonId() != null) {
                byPerson.putIfAbsent(a.getPersonId(), a);
            }
        }

        List<AttendanceResponse> rows = new ArrayList<>();
        for (Person person : persons) {
            Attendance att = byPerson.get(person.getId());
            AttendanceResponse row;
            if (att != null) {
                row = enrichResponse(attendanceMapper.toResponse(att), person);
            } else {
                row = new AttendanceResponse();
                row.setPersonId(person.getId());
                row.setAttendanceDate(filter.getDate());
                row.setDisplayStatus("NOT_CHECKED_IN");
                row.setStatus(null);
                row.setNote(null);
                enrichPersonFields(row, person);
            }
            rows.add(row);
        }

        if (SystemUtils.isNotNullOrEmpty(filter.getStatus())) {
            String want = normalizeDisplayStatus(filter.getStatus());
            rows = rows.stream()
                    .filter(r -> want.equals(normalizeDisplayStatus(r.getDisplayStatus())))
                    .toList();
        }

        int pageNum = filter.getPageNumber() != null ? filter.getPageNumber() : 1;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 20;
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1) pageSize = 20;

        long total = rows.size();
        int from = Math.min((pageNum - 1) * pageSize, rows.size());
        int to = Math.min(from + pageSize, rows.size());
        List<AttendanceResponse> slice = from < to ? rows.subList(from, to) : List.of();
        int totalPages = total == 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);

        // pageNumber 0-based — khớp PageResponse.from(Spring Page)
        return PageResponse.<AttendanceResponse>builder()
                .pageNumber(pageNum - 1)
                .pageSize(pageSize)
                .total(total)
                .totalPages(totalPages)
                .hasNext(pageNum < totalPages)
                .hasPrevious(pageNum > 1)
                .items(slice)
                .build();
    }

    private AttendanceResponse enrichResponse(AttendanceResponse row, Person person) {
        if (person == null && row.getPersonId() != null) {
            person = personRepository.findById(row.getPersonId()).orElse(null);
        }
        enrichPersonFields(row, person);
        row.setDisplayStatus(resolveDisplayStatus(row));
        return row;
    }

    private void enrichPersonFields(AttendanceResponse row, Person person) {
        if (person == null) return;
        row.setPersonName(person.getName());
        row.setDepartmentId(person.getDepartmentId());
        if (person.getDepartment() != null) {
            row.setDepartmentName(person.getDepartment().getName());
        }
    }

    /**
     * LATE ưu tiên hơn CHECKED_OUT — NV check-in muộn vẫn filter/display LATE
     * sau khi đã check-out (LNK03-08 / MOB-03).
     */
    private String resolveDisplayStatus(AttendanceResponse row) {
        if (row.getCheckInTime() == null) return "NOT_CHECKED_IN";
        boolean late = (row.getLateMinutes() != null && row.getLateMinutes() > 0)
                || row.getStatus() == AttendanceStatus.LATE;
        if (late) return "LATE";
        if (row.getCheckOutTime() != null) return "CHECKED_OUT";
        if (row.getStatus() == AttendanceStatus.ABSENT) return "ABSENT";
        if (row.getStatus() == AttendanceStatus.LEAVE) return "LEAVE";
        if (row.getStatus() == AttendanceStatus.HOLIDAY) return "HOLIDAY";
        if (row.getStatus() == AttendanceStatus.HALF_DAY) return "HALF_DAY";
        return "OK";
    }

    private String normalizeDisplayStatus(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toUpperCase();
        if ("PRESENT".equals(s) || "CHECKED_IN".equals(s)) return "OK";
        return s;
    }

    private AttendanceStatus parseAttendanceStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = normalizeDisplayStatus(raw);
        if ("OK".equals(s)) return AttendanceStatus.PRESENT;
        if ("CHECKED_OUT".equals(s) || "NOT_CHECKED_IN".equals(s)) return null;
        try {
            return AttendanceStatus.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public AttendanceResponse getById(String id) {
        return attendanceRepository.findById(id).map(attendanceMapper::toResponse)
                .orElseThrow(() -> new AppException(QlnsErrorCode.ATTENDANCE_NOT_FOUND));
    }

    @Override
    public void approve(String id, String approvedBy) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new AppException(QlnsErrorCode.ATTENDANCE_NOT_FOUND));
        attendance.setApprovedBy(approvedBy);
        attendanceRepository.save(attendance);
    }

    // ---- STATS: KPI cho Home dashboard Mobile ----
    // Tổng hợp trong 1 tháng: ngày có mặt, đi muộn, OT, phép, số phép còn lại.
    // Nếu month/year null → dùng tháng hiện tại.
    @Override
    public AttendanceStatsResponse getStats(String personId, String contractId, Integer month, Integer year) {
        if (personId == null || personId.isBlank()) {
            throw new AppException("attendance.stats.personId.required", HttpStatus.BAD_REQUEST);
        }
        java.time.LocalDate today = java.time.LocalDate.now();
        int m = month != null ? month : today.getMonthValue();
        int y = year  != null ? year  : today.getYear();

        java.time.LocalDate from = java.time.LocalDate.of(y, m, 1);
        java.time.LocalDate to   = from.withDayOfMonth(from.lengthOfMonth());

        List<Attendance> records = attendanceRepository
                .findByPersonIdAndAttendanceDateBetween(personId, from, to);

        int presentDays = 0, lateDays = 0, absentDays = 0;
        int totalWork = 0, totalLate = 0, totalOt = 0;
        java.time.LocalDate lastDate = null;
        for (Attendance a : records) {
            if (a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE) presentDays++;
            if (a.getLateMinutes() != null && a.getLateMinutes() > 0) { lateDays++; totalLate += a.getLateMinutes(); }
            if (a.getStatus() == AttendanceStatus.ABSENT) absentDays++;
            if (a.getWorkMinutes() != null) totalWork += a.getWorkMinutes();
            if (a.getOvertimeMinutes() != null) totalOt += a.getOvertimeMinutes();
            if (a.getAttendanceDate() != null && (lastDate == null || a.getAttendanceDate().isAfter(lastDate))) {
                lastDate = a.getAttendanceDate();
            }
        }

        // Số ngày làm việc chuẩn = số ngày thứ 2-6 trong tháng
        int workingDays = 0;
        for (java.time.LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            java.time.DayOfWeek dow = d.getDayOfWeek();
            if (dow != java.time.DayOfWeek.SATURDAY && dow != java.time.DayOfWeek.SUNDAY) workingDays++;
        }

        // Phép đã duyệt trong tháng + phép năm còn lại
        double leaveApproved = 0.0, leaveBalance = DEFAULT_ANNUAL_LEAVE_DAYS;
        if (contractId != null && !contractId.isBlank()) {
            leaveApproved = leaveRequestRepository.sumApprovedLeavesByContractAndPeriod(
                    contractId, from, to);
            double usedYear = leaveRequestRepository.sumApprovedLeavesByTypeAndPeriod(
                    contractId, "ANNUAL",
                    java.time.LocalDate.of(y, 1, 1),
                    java.time.LocalDate.of(y, 12, 31));
            leaveBalance = Math.max(DEFAULT_ANNUAL_LEAVE_DAYS - usedYear, 0.0);
        }

        return AttendanceStatsResponse.builder()
                .month(m).year(y)
                .workingDays(workingDays)
                .presentDays(presentDays)
                .lateDays(lateDays)
                .absentDays(absentDays)
                .totalWorkMinutes(totalWork)
                .totalLateMinutes(totalLate)
                .totalOvertimeMinutes(totalOt)
                .leaveDaysApproved(leaveApproved)
                .leaveBalance(leaveBalance)
                .lastAttendanceDate(lastDate != null ? lastDate.toString() : null)
                .build();
    }
}
