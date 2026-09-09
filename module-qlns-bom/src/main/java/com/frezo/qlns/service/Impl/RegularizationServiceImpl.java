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
    /** Dùng chung helper danh tính/scope (personId hiện tại, admin, HR, QL trực tiếp). */
    private final LeaveApprovalResolver accessResolver;

    @Value("${frezo.leave.hr-users:admin}")
    private String hrUsersCsv;

    /**
     * Tạo đơn giải trình chấm công mới ở trạng thái PENDING, xác định người duyệt mặc định (quản lý hoặc HR đầu tiên) và gửi thông báo.
     */
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

    /**
     * Liệt kê các đơn giải trình của một nhân viên, kiểm tra quyền xem trước khi trả dữ liệu.
     */
    @Override
    public List<AttendanceRegularization> myRequests(String personId) {
        assertCanViewPersonRequests(personId);
        return repo.findByPersonIdOrderByCreatedDateDesc(personId);
    }

    /**
     * Chống IDOR: chỉ chính chủ (hoặc admin / HR / QL trực tiếp) đọc được đơn giải trình
     * theo {@code personId}. Người duyệt dùng {@code /pending} nên không cần ngoại lệ rộng hơn.
     */
    private void assertCanViewPersonRequests(String personId) {
        if (personId == null || personId.isBlank()) {
            throw new AppException("regularization.invalid.input", HttpStatus.BAD_REQUEST);
        }
        if (accessResolver.isCurrentUserAdmin()
                || accessResolver.isCurrentUserHr()
                || accessResolver.isDirectManagerOf(personId)) {
            return;
        }
        String me = accessResolver.currentPersonId();
        if (me != null && me.equals(personId)) return;
        throw new AppException("regularization.view.denied", HttpStatus.FORBIDDEN);
    }

    /**
     * Lấy danh sách đơn đang chờ duyệt (PENDING) được giao cho một người duyệt cụ thể.
     */
    @Override
    public List<AttendanceRegularization> pendingForManager(String managerUsername) {
        return repo.findByManagerUsernameAndStatusOrderByCreatedDateDesc(managerUsername, "PENDING");
    }

    /**
     * Duyệt đơn giải trình: chuyển trạng thái sang APPROVED, áp dụng giờ chấm công yêu cầu vào bản ghi Attendance của ngày đó và thông báo cho người gửi.
     */
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

    /**
     * Từ chối đơn giải trình: chuyển trạng thái sang REJECTED kèm lý do và thông báo cho người gửi.
     */
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

    /**
     * Tìm đơn giải trình theo ID, ném lỗi NOT_FOUND nếu không tồn tại.
     */
    private AttendanceRegularization mustExist(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new AppException("regularization.not.found", HttpStatus.NOT_FOUND));
    }

    /**
     * Cập nhật giờ check-in/check-out đã duyệt vào bản ghi chấm công của ngày tương ứng, tạo mới bản ghi nếu chưa có và ghi chú giải trình.
     */
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

    /**
     * Lấy tên người dùng HR đầu tiên từ cấu hình CSV, dùng làm người duyệt mặc định khi không có quản lý.
     */
    private String firstHrUser() {
        if (hrUsersCsv == null || hrUsersCsv.isBlank()) return null;
        String[] arr = hrUsersCsv.split(",");
        return arr.length > 0 ? arr[0].trim() : null;
    }

    /**
     * Cắt chuỗi về độ dài tối đa (thêm dấu "...") để dùng cho nội dung thông báo ngắn gọn.
     */
    private String trunc(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
