package com.frezo.qlns.job;

import com.frezo.auth.entity.User;
import com.frezo.common.scheduling.SchedulableJob;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.repository.NotificationRepository;
import com.frezo.common.service.NotificationService;
import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.entity.Attendance;
import com.frezo.qlns.repository.AttendanceRepository;
import com.frezo.qlns.repository.LeaveRequestRepository;
import com.frezo.qtht.entity.Setting;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Cron reminder chấm công — chạy mỗi 5 phút.
 * <p>
 * Rule:
 * - Sau giờ bắt đầu ca 5–15 phút → nhắc user có ngày làm nhưng chưa check-in.
 * - Cửa sổ 30p SAU giờ end ca chiều → push "Đừng quên check-out" cho user
 *   đã check-in nhưng chưa check-out hôm nay.
 * <p>
 * NotificationService là single delivery path: lưu DB + WebSocket chuông + mobile push.
 * Dedup kiểm tra DB theo username/type/entityId nên sống qua restart và giảm trùng multi-instance.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceReminderJob implements SchedulableJob {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final PersonRepository personRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public String getCode() {
        return "ATTENDANCE_REMINDER";
    }

    @Override
    public String getDisplayName() {
        return "Nhắc chấm công";
    }

    @Override
    public String getDescription() {
        return "Nhắc nhân viên check-in trước giờ vào ca và check-out sau giờ tan ca";
    }

    @Override
    public String getModuleCode() {
        return "QLNS";
    }

    @Override
    public String getDefaultCron() {
        return "0 */5 * * * MON-FRI";
    }

    @Override
    public void execute() {
        tick();
    }

    public void tick() {
        LocalDate today = LocalDate.now();
        DayOfWeek dow = today.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return;

        LocalTime now = LocalTime.now();
        try {
            processCheckInReminder(today, now);
            processCheckOutReminder(today, now);
        } catch (Exception e) {
            log.error("[Reminder] tick failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Nhắc check-in 5–15 phút sau giờ bắt đầu ca.
     */
    private void processCheckInReminder(LocalDate today, LocalTime now) {
        // Group user theo orgId để 1 lần query setting/org
        Map<String, List<User>> usersByOrg = groupActiveUsersByOrg();

        for (Map.Entry<String, List<User>> entry : usersByOrg.entrySet()) {
            String orgId = entry.getKey();
            LocalTime morningStart = getMorningStart(orgId);
            long minutesAfterStart = java.time.temporal.ChronoUnit.MINUTES.between(morningStart, now);
            if (minutesAfterStart < 5 || minutesAfterStart > 15) continue;

            for (User u : entry.getValue()) {
                if (!isEligibleWorkDay(u, today)) continue;
                Optional<Attendance> att = attendanceRepository
                        .findByPersonIdAndAttendanceDate(u.getPersonId(), today);
                if (att.isPresent() && (att.get().getStatus() == AttendanceStatus.HOLIDAY
                        || att.get().getStatus() == AttendanceStatus.LEAVE)) continue;
                if (att.isPresent() && att.get().getCheckInTime() != null) continue;
                String entityId = today + ":IN";
                if (alreadySent(u.getUserName(), "ATTENDANCE_CHECK_IN_REMINDER", entityId)) continue;
                notificationService.notify(
                        u.getUserName(),
                        "Bạn chưa chấm công vào",
                        "Ca làm bắt đầu lúc " + morningStart.format(TIME_FMT) + " — mở chấm công để check-in.",
                        "ATTENDANCE_CHECK_IN_REMINDER",
                        "ATTENDANCE",
                        entityId,
                        "/attendance",
                        null,
                        false);
                log.info("[Reminder] Sent check-in reminder user={} org={}", u.getUserName(), orgId);
            }
        }
    }

    /**
     * Push "Đừng quên check-out" cho user đã check-in mà đến giờ end ca chiều +30p vẫn chưa check-out.
     */
    private void processCheckOutReminder(LocalDate today, LocalTime now) {
        Map<String, List<User>> usersByOrg = groupActiveUsersByOrg();

        for (Map.Entry<String, List<User>> entry : usersByOrg.entrySet()) {
            String orgId = entry.getKey();
            LocalTime afternoonEnd = getAfternoonEnd(orgId);
            LocalTime cutoff = afternoonEnd.plusMinutes(30);
            if (now.isBefore(cutoff)) continue;
            // Chỉ nhắc trong 1 giờ sau cutoff (tránh spam tối muộn)
            if (java.time.temporal.ChronoUnit.MINUTES.between(cutoff, now) > 60) continue;

            for (User u : entry.getValue()) {
                if (!isEligibleWorkDay(u, today)) continue;
                Optional<Attendance> att = attendanceRepository
                        .findByPersonIdAndAttendanceDate(u.getPersonId(), today);
                if (att.isEmpty()) continue;                          // Chưa check-in → bỏ qua
                if (att.get().getCheckOutTime() != null) continue;    // Đã check-out → OK
                if (att.get().getCheckInTime() == null) continue;
                String entityId = today + ":OUT";
                if (alreadySent(u.getUserName(), "ATTENDANCE_CHECK_OUT_REMINDER", entityId)) continue;
                notificationService.notify(
                        u.getUserName(),
                        "Bạn chưa chấm công ra",
                        "Ca làm đã kết thúc lúc " + afternoonEnd.format(TIME_FMT) + " — mở chấm công để check-out.",
                        "ATTENDANCE_CHECK_OUT_REMINDER",
                        "ATTENDANCE",
                        entityId,
                        "/attendance",
                        null,
                        false);
                log.info("[Reminder] Sent check-out reminder user={} org={}", u.getUserName(), orgId);
            }
        }
    }

    // ---- Helpers ----

    private Map<String, List<User>> groupActiveUsersByOrg() {
        // Load hết User có personId (giả định số lượng < 10k — nếu lớn cần repository projection).
        List<User> users = userRepository.findAll();
        Map<String, List<User>> byOrg = new HashMap<>();
        for (User u : users) {
            if (u.getPersonId() == null || u.getUserName() == null) continue;
            Person person = personRepository.findByIdAndIsDeletedFalse(u.getPersonId()).orElse(null);
            if (person == null || !Boolean.TRUE.equals(person.getActivated())) continue;
            String orgId = person.getOrgId() != null ? person.getOrgId() : "DEFAULT";
            byOrg.computeIfAbsent(orgId, k -> new ArrayList<>()).add(u);
        }
        return byOrg;
    }

    private boolean isEligibleWorkDay(User user, LocalDate date) {
        if (user == null || user.getPersonId() == null || user.getUserName() == null) return false;
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) return false;
        return leaveRequestRepository
                .findApprovedOverlapping(user.getPersonId(), date, date)
                .isEmpty();
    }

    private boolean alreadySent(String username, String type, String entityId) {
        return notificationRepository.existsByUsernameAndTypeAndEntityId(username, type, entityId);
    }

    private LocalTime getMorningStart(String orgId) {
        return parseTime(readSetting(orgId, Setting::getMorningStart), LocalTime.of(8, 0));
    }

    private LocalTime getAfternoonEnd(String orgId) {
        return parseTime(readSetting(orgId, Setting::getAfternoonEnd), LocalTime.of(17, 30));
    }

    private String readSetting(String orgId, java.util.function.Function<Setting, String> getter) {
        if (orgId == null || "DEFAULT".equals(orgId)) return null;
        return settingRepository.findByOrgIdAndIsDeletedFalse(orgId)
                .map(getter).orElse(null);
    }

    private LocalTime parseTime(String val, LocalTime fallback) {
        if (val == null || val.isBlank()) return fallback;
        try { return LocalTime.parse(val.trim(), TIME_FMT); }
        catch (Exception e) { return fallback; }
    }
}
