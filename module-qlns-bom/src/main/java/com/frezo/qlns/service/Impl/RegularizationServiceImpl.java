package com.frezo.qlns.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.service.NotificationService;
import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.dto.request.RegularizationAddRequest;
import com.frezo.qlns.entity.Attendance;
import com.frezo.qlns.entity.AttendanceRegularization;
import com.frezo.qlns.repository.AttendanceRegularizationRepository;
import com.frezo.qlns.repository.AttendanceRepository;
import com.frezo.qlns.service.RegularizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Workflow đơn giải trình chấm công — 1 tầng duyệt (manager hoặc HR).
 * Khi APPROVED: tự cập nhật Attendance record của ngày đó (tạo mới nếu chưa có).
 * Notification qua {@link NotificationService} — không phá contract cũ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegularizationServiceImpl implements RegularizationService {

    private final AttendanceRegularizationRepository repo;
    private final AttendanceRepository attendanceRepository;
    private final NotificationService notificationService;

    @Value("${frezo.leave.hr-users:admin}")
    private String hrUsersCsv;

    @Override
    public AttendanceRegularization create(RegularizationAddRequest req) {
        if (req.getPersonId() == null || req.getAttendanceDate() == null || req.getReason() == null) {
            throw new AppException("regularization.invalid.input", HttpStatus.BAD_REQUEST);
        }
        if (req.getRequestedCheckIn() == null && req.getRequestedCheckOut() == null) {
            throw new AppException("regularization.time.required", HttpStatus.BAD_REQUEST);
        }
        String actor = SystemUtils.getCurrentUsername();
        String manager = req.getManagerUsername();
        if (manager == null || manager.isBlank()) {
            manager = firstHrUser();
        }

        AttendanceRegularization entity = AttendanceRegularization.builder()
                .personId(req.getPersonId())
                .contractId(req.getContractId())
                .attendanceDate(req.getAttendanceDate())
                .requestedCheckIn(req.getRequestedCheckIn())
                .requestedCheckOut(req.getRequestedCheckOut())
                .reason(req.getReason())
                .status("PENDING")
                .managerUsername(manager)
                .build();
        entity.setCreatedBy(actor);
        AttendanceRegularization saved = repo.save(entity);

        if (manager != null) {
            notificationService.notify(
                    manager,
                    "Đơn giải trình chấm công mới",
                    "Nhân viên " + actor + " gửi đơn giải trình ngày "
                            + req.getAttendanceDate() + " · Lý do: " + trunc(req.getReason(), 120),
                    "REGULARIZATION_PENDING", "ATTENDANCE_REGULARIZATION", saved.getId(),
                    "/qlns/attendance/regularization/" + saved.getId(), actor, false);
        }
        return saved;
    }

    @Override
    public List<AttendanceRegularization> myRequests(String personId) {
        return repo.findByPersonIdOrderByCreatedDateDesc(personId);
    }

    @Override
    public List<AttendanceRegularization> pendingForManager(String managerUsername) {
        return repo.findByManagerUsernameAndStatusOrderByCreatedDateDesc(managerUsername, "PENDING");
    }

    @Override
    public AttendanceRegularization approve(String id) {
        AttendanceRegularization e = mustExist(id);
        if (!"PENDING".equals(e.getStatus())) {
            throw new AppException("regularization.status.invalid", HttpStatus.CONFLICT);
        }
        String actor = SystemUtils.getCurrentUsername();
        e.setStatus("APPROVED");
        e.setApprovedBy(actor);
        e.setApprovedAt(LocalDateTime.now());
        AttendanceRegularization saved = repo.save(e);

        // Áp dụng vào Attendance record của ngày đó.
        applyToAttendance(saved);

        // Notify requester
        if (saved.getCreatedBy() != null) {
            notificationService.notify(
                    saved.getCreatedBy(),
                    "Đơn giải trình chấm công đã được duyệt",
                    "Quản lý " + actor + " đã duyệt đơn ngày " + saved.getAttendanceDate(),
                    "REGULARIZATION_APPROVED", "ATTENDANCE_REGULARIZATION", saved.getId(),
                    "/qlns/attendance/regularization/" + saved.getId(), actor, true);
        }
        return saved;
    }

    @Override
    public AttendanceRegularization reject(String id, String reason) {
        AttendanceRegularization e = mustExist(id);
        if (!"PENDING".equals(e.getStatus())) {
            throw new AppException("regularization.status.invalid", HttpStatus.CONFLICT);
        }
        String actor = SystemUtils.getCurrentUsername();
        e.setStatus("REJECTED");
        e.setRejectedBy(actor);
        e.setRejectedAt(LocalDateTime.now());
        e.setRejectReason(reason);
        AttendanceRegularization saved = repo.save(e);

        if (saved.getCreatedBy() != null) {
            notificationService.notify(
                    saved.getCreatedBy(),
                    "Đơn giải trình bị từ chối",
                    "Quản lý " + actor + " từ chối · " + trunc(reason, 140),
                    "REGULARIZATION_REJECTED", "ATTENDANCE_REGULARIZATION", saved.getId(),
                    "/qlns/attendance/regularization/" + saved.getId(), actor, true);
        }
        return saved;
    }

    // ---------- Helpers ----------

    private AttendanceRegularization mustExist(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new AppException("regularization.not.found", HttpStatus.NOT_FOUND));
    }

    private void applyToAttendance(AttendanceRegularization r) {
        Attendance a = attendanceRepository
                .findByPersonIdAndAttendanceDate(r.getPersonId(), r.getAttendanceDate())
                .orElseGet(() -> Attendance.builder()
                        .personId(r.getPersonId())
                        .contractId(r.getContractId())
                        .attendanceDate(r.getAttendanceDate())
                        .build());

        if (r.getRequestedCheckIn() != null) a.setCheckInTime(r.getRequestedCheckIn());
        if (r.getRequestedCheckOut() != null) a.setCheckOutTime(r.getRequestedCheckOut());
        if (a.getStatus() == null) a.setStatus(AttendanceStatus.PRESENT);
        a.setApprovedBy(r.getApprovedBy());
        a.setNote((a.getNote() != null ? a.getNote() + " | " : "")
                + "[Giải trình] " + trunc(r.getReason(), 200));
        attendanceRepository.save(a);
    }

    private String firstHrUser() {
        if (hrUsersCsv == null || hrUsersCsv.isBlank()) return null;
        String[] arr = hrUsersCsv.split(",");
        return arr.length > 0 ? arr[0].trim() : null;
    }

    private String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
