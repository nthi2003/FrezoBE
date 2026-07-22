package com.frezo.qlns.job;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.qlns.entity.Attendance;
import com.frezo.qlns.repository.AttendanceRepository;
import com.frezo.qtht.entity.Setting;
import com.frezo.qtht.repository.SettingRepository;
import com.frezo.qtht.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
 * - Cửa sổ 15p TRƯỚC giờ start ca sáng → push "Chuẩn bị check-in" cho user
 *   thuộc org (có personId) chưa có Attendance record hôm nay.
 * - Cửa sổ 30p SAU giờ end ca chiều → push "Đừng quên check-out" cho user
 *   đã check-in nhưng chưa check-out hôm nay.
 * <p>
 * Không spam: mỗi ngày mỗi user chỉ nhận 1 reminder cho mỗi loại (in-memory dedup).
 * Đơn giản đủ cho MVP+ — production nên dùng Redis TTL cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AttendanceReminderJob {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SettingRepository settingRepository;
    private final PushNotificationService pushService;

    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** Dedup key: "checkin::{personId}::{date}" — reset mỗi ngày trong sweep(). */
    private final Set<String> sentToday = ConcurrentHashSetOf();

    private static Set<String> ConcurrentHashSetOf() {
        return java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    }

    private volatile LocalDate lastSweepDate = LocalDate.now();

    @Scheduled(cron = "0 */5 * * * MON-FRI")
    public void tick() {
        LocalDate today = LocalDate.now();
        // Reset dedup mỗi ngày
        if (!today.equals(lastSweepDate)) {
            sentToday.clear();
            lastSweepDate = today;
        }

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
     * Push "Chuẩn bị check-in" cho user thuộc org có morning_start trong khoảng [now, now+15p]
     * và chưa có Attendance record hôm nay.
     */
    private void processCheckInReminder(LocalDate today, LocalTime now) {
        // Group user theo orgId để 1 lần query setting/org
        Map<String, List<User>> usersByOrg = groupActiveUsersByOrg();

        for (Map.Entry<String, List<User>> entry : usersByOrg.entrySet()) {
            String orgId = entry.getKey();
            LocalTime morningStart = getMorningStart(orgId);
            long minutesUntilStart = java.time.temporal.ChronoUnit.MINUTES.between(now, morningStart);
            if (minutesUntilStart < 0 || minutesUntilStart > 15) continue;

            // Tìm user chưa check-in
            List<String> usersToNotify = new ArrayList<>();
            for (User u : entry.getValue()) {
                if (u.getUserName() == null || u.getPersonId() == null) continue;
                String dedupKey = "checkin::" + u.getPersonId() + "::" + today;
                if (sentToday.contains(dedupKey)) continue;
                Optional<Attendance> att = attendanceRepository
                        .findByPersonIdAndAttendanceDate(u.getPersonId(), today);
                if (att.isPresent() && att.get().getCheckInTime() != null) continue;
                usersToNotify.add(u.getUserName());
                sentToday.add(dedupKey);
            }

            if (!usersToNotify.isEmpty()) {
                pushService.sendToUsers(usersToNotify,
                        "Chuẩn bị chấm công",
                        "Ca sáng bắt đầu lúc " + morningStart.format(TIME_FMT) + " — mở app để check-in nhé.",
                        Map.of("type", "CHECK_IN_REMINDER", "href", "/attendance/check-in"));
                log.info("[Reminder] Sent check-in reminder to {} users (org={})", usersToNotify.size(), orgId);
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

            List<String> usersToNotify = new ArrayList<>();
            for (User u : entry.getValue()) {
                if (u.getUserName() == null || u.getPersonId() == null) continue;
                String dedupKey = "checkout::" + u.getPersonId() + "::" + today;
                if (sentToday.contains(dedupKey)) continue;
                Optional<Attendance> att = attendanceRepository
                        .findByPersonIdAndAttendanceDate(u.getPersonId(), today);
                if (att.isEmpty()) continue;                          // Chưa check-in → bỏ qua
                if (att.get().getCheckOutTime() != null) continue;    // Đã check-out → OK
                if (att.get().getCheckInTime() == null) continue;
                usersToNotify.add(u.getUserName());
                sentToday.add(dedupKey);
            }

            if (!usersToNotify.isEmpty()) {
                pushService.sendToUsers(usersToNotify,
                        "Bạn quên check-out rồi",
                        "Ca chiều đã kết thúc lúc " + afternoonEnd.format(TIME_FMT) + " — mở app để check-out.",
                        Map.of("type", "CHECK_OUT_REMINDER", "href", "/attendance/check-in"));
                log.info("[Reminder] Sent check-out reminder to {} users (org={})", usersToNotify.size(), orgId);
            }
        }
    }

    // ---- Helpers ----

    private Map<String, List<User>> groupActiveUsersByOrg() {
        // Load hết User có personId (giả định số lượng < 10k — nếu lớn cần join Person.orgId qua @Query).
        // Simplified: chỉ dùng personId → resolve org 1 lần. Ở prod nên cache.
        List<User> users = userRepository.findAll();
        Map<String, List<User>> byOrg = new HashMap<>();
        for (User u : users) {
            if (u.getPersonId() == null || u.getUserName() == null) continue;
            // Do không có method personRepository.findById nhanh trong context này, group tạm theo "DEFAULT".
            byOrg.computeIfAbsent("DEFAULT", k -> new ArrayList<>()).add(u);
        }
        return byOrg;
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
