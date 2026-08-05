package com.frezo.qtht.job;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.scheduling.JobExecutionException;
import com.frezo.common.scheduling.SchedulableJob;
import com.frezo.qtht.entity.SystemJob;
import com.frezo.qtht.entity.SystemJobHistory;
import com.frezo.qtht.repository.SystemJobHistoryRepository;
import com.frezo.qtht.repository.SystemJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Đăng ký {@link SchedulableJob} theo cron lưu trong bảng {@code system_job} — thay cho
 * {@code @Scheduled} cứng trong code.
 * <p>
 * Vòng đời: {@link ApplicationReadyEvent} → seed row còn thiếu → schedule job đang bật.
 * Mỗi lần admin sửa cron / bật tắt, {@code SystemJobService} gọi {@link #reschedule(String)}.
 * <p>
 * Một job không chạy chồng: cron trigger gặp lượt đang chạy sẽ ghi SKIPPED, chạy tay sẽ báo CONFLICT.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicJobScheduler {

    public static final String TRIGGERED_BY_SYSTEM = "SYSTEM";

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final ObjectProvider<SchedulableJob> jobProvider;
    private final ThreadPoolTaskScheduler systemJobTaskScheduler;
    private final SystemJobRepository jobRepository;
    private final SystemJobHistoryRepository historyRepository;

    private final Map<String, SchedulableJob> catalog = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();
    private final Set<String> running = ConcurrentHashMap.newKeySet();

    // ------------------------------------------------------------
    // Bootstrap
    // ------------------------------------------------------------

    @EventListener(ApplicationReadyEvent.class)
    public void bootstrap() {
        jobProvider.orderedStream().forEach(bean -> catalog.put(bean.getCode(), bean));
        log.info("[JobScheduler] tìm thấy {} job trong catalog: {}", catalog.size(), catalog.keySet());

        catalog.values().forEach(this::syncCatalogRow);
        jobRepository.findByIsDeletedFalseOrderByModuleCodeAscJobNameAsc().forEach(this::applySchedule);
    }

    /** Tạo row DB từ metadata bean nếu chưa có; nếu có rồi chỉ đồng bộ phần mô tả (giữ nguyên cron admin đã chỉnh). */
    private void syncCatalogRow(SchedulableJob bean) {
        SystemJob entity = jobRepository.findByJobCodeAndIsDeletedFalse(bean.getCode()).orElse(null);
        if (entity == null) {
            entity = SystemJob.builder()
                    .jobCode(bean.getCode())
                    .jobName(bean.getDisplayName())
                    .description(bean.getDescription())
                    .moduleCode(bean.getModuleCode())
                    .cronExpression(bean.getDefaultCron())
                    .enabled(true)
                    .build();
            entity.setCreatedBy(TRIGGERED_BY_SYSTEM);
        } else {
            entity.setJobName(bean.getDisplayName());
            entity.setDescription(bean.getDescription());
            entity.setModuleCode(bean.getModuleCode());
        }
        jobRepository.save(entity);
    }

    // ------------------------------------------------------------
    // Schedule control
    // ------------------------------------------------------------

    /** Huỷ lịch cũ rồi đăng ký lại theo cấu hình hiện tại của row. */
    public void applySchedule(SystemJob job) {
        String code = job.getJobCode();
        cancel(code);

        if (!Boolean.TRUE.equals(job.getEnabled())) {
            job.setNextRunAt(null);
            jobRepository.save(job);
            return;
        }
        if (!catalog.containsKey(code)) {
            log.warn("[JobScheduler] job {} có trong DB nhưng không có bean tương ứng — bỏ qua", code);
            return;
        }
        try {
            ScheduledFuture<?> future = systemJobTaskScheduler.schedule(
                    () -> runFromCron(code), new CronTrigger(job.getCronExpression()));
            if (future != null) {
                futures.put(code, future);
            }
            job.setNextRunAt(nextRunAt(job.getCronExpression()));
            jobRepository.save(job);
            log.info("[JobScheduler] đã lên lịch {} với cron '{}' (kế tiếp {})",
                    code, job.getCronExpression(), job.getNextRunAt());
        } catch (Exception e) {
            log.error("[JobScheduler] không lên lịch được job {} với cron '{}': {}",
                    code, job.getCronExpression(), e.getMessage(), e);
        }
    }

    public void reschedule(String code) {
        jobRepository.findByJobCodeAndIsDeletedFalse(code).ifPresent(this::applySchedule);
    }

    private void cancel(String code) {
        ScheduledFuture<?> future = futures.remove(code);
        if (future != null) {
            future.cancel(false);
        }
    }

    // ------------------------------------------------------------
    // Execution
    // ------------------------------------------------------------

    /** Chạy tay từ màn hình quản trị — async trên pool riêng, trả về ngay. */
    public void runNow(String code, String triggeredBy) {
        if (!catalog.containsKey(code)) {
            throw new AppException(CommonErrorCode.NOT_FOUND, code);
        }
        if (!running.add(code)) {
            throw new AppException(CommonErrorCode.CONFLICT, "Tác vụ đang chạy, vui lòng đợi kết thúc: " + code);
        }
        try {
            systemJobTaskScheduler.execute(() -> {
                try {
                    execute(code, triggeredBy == null ? TRIGGERED_BY_SYSTEM : triggeredBy);
                } finally {
                    running.remove(code);
                }
            });
        } catch (RuntimeException e) {
            running.remove(code);
            throw e;
        }
    }

    private void runFromCron(String code) {
        if (!running.add(code)) {
            log.warn("[JobScheduler] job {} vẫn đang chạy — bỏ qua lượt này", code);
            recordSkipped(code);
            return;
        }
        try {
            execute(code, TRIGGERED_BY_SYSTEM);
        } finally {
            running.remove(code);
        }
    }

    private void execute(String code, String triggeredBy) {
        SchedulableJob bean = catalog.get(code);
        if (bean == null) {
            log.warn("[JobScheduler] không tìm thấy bean cho job {}", code);
            return;
        }
        LocalDateTime startedAt = LocalDateTime.now();
        SystemJobHistory history = historyRepository.save(SystemJobHistory.builder()
                .jobCode(code)
                .startedAt(startedAt)
                .status(STATUS_RUNNING)
                .triggeredBy(triggeredBy)
                .build());

        long start = System.currentTimeMillis();
        String status = STATUS_SUCCESS;
        String message = null;
        try {
            bean.execute();
        } catch (Exception e) {
            status = STATUS_FAILED;
            message = buildFailureMessage(e);
            log.error("[JobScheduler] job {} lỗi: {}", code, e.getMessage(), e);
        }

        long durationMs = System.currentTimeMillis() - start;
        history.setFinishedAt(LocalDateTime.now());
        history.setDurationMs(durationMs);
        history.setStatus(status);
        history.setMessage(truncate(message));
        historyRepository.save(history);

        updateLastRun(code, startedAt, status, durationMs, truncate(message));
    }

    private void recordSkipped(String code) {
        LocalDateTime now = LocalDateTime.now();
        historyRepository.save(SystemJobHistory.builder()
                .jobCode(code)
                .startedAt(now)
                .finishedAt(now)
                .durationMs(0L)
                .status(STATUS_SKIPPED)
                .message("Lượt chạy bị bỏ qua vì tác vụ trước chưa kết thúc")
                .triggeredBy(TRIGGERED_BY_SYSTEM)
                .build());
    }

    private void updateLastRun(String code, LocalDateTime startedAt, String status, long durationMs, String message) {
        jobRepository.findByJobCodeAndIsDeletedFalse(code).ifPresent(job -> {
            job.setLastRunAt(startedAt);
            job.setLastStatus(status);
            job.setLastDurationMs(durationMs);
            job.setLastMessage(message);
            job.setNextRunAt(Boolean.TRUE.equals(job.getEnabled()) ? nextRunAt(job.getCronExpression()) : null);
            jobRepository.save(job);
        });
    }

    // ------------------------------------------------------------
    // Query helpers cho service
    // ------------------------------------------------------------

    public boolean isRunning(String code) {
        return running.contains(code);
    }

    public boolean isKnownJob(String code) {
        return catalog.containsKey(code);
    }

    /**
     * Lý do job chưa chạy được (thiếu công cụ / thiếu cấu hình), hoặc {@code null} nếu sẵn sàng.
     * Lỗi khi tự kiểm tra cũng coi là chưa sẵn sàng để màn hình quản trị không im lặng.
     */
    public String readinessMessage(String code) {
        SchedulableJob bean = catalog.get(code);
        if (bean == null) {
            return null;
        }
        try {
            return bean.checkReadiness();
        } catch (Exception e) {
            log.warn("[JobScheduler] không kiểm tra được điều kiện chạy của job {}: {}", code, e.getMessage());
            return "Không kiểm tra được điều kiện chạy của tác vụ. Xem nhật ký hệ thống để biết chi tiết.";
        }
    }

    public Collection<SchedulableJob> catalog() {
        return catalog.values();
    }

    public static LocalDateTime nextRunAt(String cronExpression) {
        return Optional.ofNullable(cronExpression)
                .filter(CronExpression::isValidExpression)
                .map(cron -> CronExpression.parse(cron).next(LocalDateTime.now()))
                .orElse(null);
    }

    /**
     * Dòng đầu là câu tiếng Việt cho quản trị viên, dòng sau giữ chi tiết kỹ thuật để dò lỗi.
     * Màn hình danh sách chỉ hiện dòng đầu; phần chi tiết nằm trong popup lịch sử chạy.
     */
    private static String buildFailureMessage(Exception e) {
        String friendly = e instanceof JobExecutionException
                ? e.getMessage()
                : "Tác vụ chạy thất bại do lỗi hệ thống. Hãy xem chi tiết kỹ thuật bên dưới hoặc nhật ký máy chủ.";
        String detail = e.getClass().getSimpleName()
                + (e.getMessage() == null ? "" : ": " + e.getMessage());
        return friendly + "\nChi tiết kỹ thuật: " + detail;
    }

    private static String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH);
    }
}
