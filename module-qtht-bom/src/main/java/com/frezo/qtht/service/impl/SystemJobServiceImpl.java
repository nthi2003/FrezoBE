package com.frezo.qtht.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.FePage;
import com.frezo.qtht.dto.request.SystemJobUpdateRequest;
import com.frezo.qtht.dto.response.SystemJobDto;
import com.frezo.qtht.dto.response.SystemJobHistoryDto;
import com.frezo.qtht.entity.SystemJob;
import com.frezo.qtht.entity.SystemJobHistory;
import com.frezo.qtht.job.DynamicJobScheduler;
import com.frezo.qtht.repository.SystemJobHistoryRepository;
import com.frezo.qtht.repository.SystemJobRepository;
import com.frezo.qtht.service.SystemJobService;
import com.frezo.qtht.util.CronDescriptionUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemJobServiceImpl implements SystemJobService {

    /**
     * Trạng thái vận hành của job — cố ý KHÔNG gộp kết quả lần chạy gần nhất vào đây.
     * Kết quả (SUCCESS/FAILED/SKIPPED) nằm ở {@code lastStatus} để màn hình quản trị không
     * hiển thị hai nhãn mâu thuẫn ("Lỗi" cạnh "Thất bại").
     */
    private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_DISABLED = "DISABLED";
    private static final String STATUS_RUNNING = "RUNNING";
    /** Job đang bật nhưng thiếu điều kiện chạy (công cụ ngoài, tệp cấu hình...). */
    private static final String STATUS_NOT_READY = "NOT_READY";

    private static final int MAX_PREVIEW_COUNT = 10;
    private static final int DEFAULT_PREVIEW_COUNT = 5;

    private final SystemJobRepository jobRepository;
    private final SystemJobHistoryRepository historyRepository;
    private final DynamicJobScheduler scheduler;

    @Override
    public List<SystemJobDto> listJobs() {
        return jobRepository.findByIsDeletedFalseOrderByModuleCodeAscJobNameAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public SystemJobDto updateJob(String code, SystemJobUpdateRequest request) {
        SystemJob job = findJob(code);

        if (request == null || (request.getCronExpression() == null && request.getEnabled() == null)) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST,
                    "Cần ít nhất một thay đổi: cronExpression hoặc enabled");
        }

        if (request.getCronExpression() != null) {
            String cron = request.getCronExpression().trim();
            try {
                CronExpression.parse(cron);
            } catch (IllegalArgumentException e) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST,
                        "Biểu thức cron không hợp lệ: " + cron);
            }
            job.setCronExpression(cron);
        }
        if (request.getEnabled() != null) {
            job.setEnabled(request.getEnabled());
        }

        jobRepository.save(job);
        scheduler.applySchedule(job);
        log.info("[SystemJob] {} cập nhật bởi {} — cron '{}', enabled={}",
                code, SystemUtils.getCurrentUsername(), job.getCronExpression(), job.getEnabled());
        return toDto(job);
    }

    @Override
    public SystemJobDto runNow(String code) {
        SystemJob job = findJob(code);
        String triggeredBy = SystemUtils.getCurrentUsername();
        scheduler.runNow(job.getJobCode(), triggeredBy);
        return toDto(job);
    }

    @Override
    public FePage<SystemJobHistoryDto> history(String code, Integer pageNumber, Integer pageSize,
                                               String status, LocalDateTime fromDate, LocalDateTime toDate) {
        Sort sort = Sort.by(Sort.Direction.DESC, "startedAt");
        Page<SystemJobHistory> page = historyRepository.findAll(
                historySpecification(code, status, fromDate, toDate),
                ServiceHelper.createPageable(pageNumber, pageSize == null ? 20 : pageSize, sort));

        return FePage.<SystemJobHistoryDto>builder()
                .content(page.getContent().stream().map(this::toHistoryDto).toList())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .number(page.getNumber())
                .size(page.getSize())
                .build();
    }

    @Override
    public List<String> previewCron(String expression, Integer count) {
        if (expression == null || expression.isBlank()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Thiếu tham số expression");
        }
        String cron = expression.trim();
        CronExpression parsed;
        try {
            parsed = CronExpression.parse(cron);
        } catch (IllegalArgumentException e) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Biểu thức cron không hợp lệ: " + cron);
        }

        int limit = count == null ? DEFAULT_PREVIEW_COUNT : Math.min(Math.max(count, 1), MAX_PREVIEW_COUNT);
        List<String> result = new ArrayList<>(limit);
        LocalDateTime cursor = LocalDateTime.now();
        for (int i = 0; i < limit; i++) {
            cursor = parsed.next(cursor);
            if (cursor == null) {
                break;
            }
            result.add(cursor.toString());
        }
        return result;
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------

    private SystemJob findJob(String code) {
        return jobRepository.findByJobCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, code));
    }

    private Specification<SystemJobHistory> historySpecification(String code, String status,
                                                                 LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("jobCode"), code));
            predicates.add(cb.isFalse(root.get("isDeleted")));
            if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
                predicates.add(cb.equal(root.get("status"), status.toUpperCase()));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startedAt"), toDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private SystemJobDto toDto(SystemJob job) {
        String readiness = scheduler.readinessMessage(job.getJobCode());
        return SystemJobDto.builder()
                .code(job.getJobCode())
                .name(job.getJobName())
                .description(job.getDescription())
                .moduleCode(job.getModuleCode())
                .cronExpression(job.getCronExpression())
                .cronDescription(CronDescriptionUtil.describe(job.getCronExpression()))
                .enabled(Boolean.TRUE.equals(job.getEnabled()))
                .status(resolveStatus(job, readiness))
                .readinessMessage(readiness)
                .lastRunAt(job.getLastRunAt())
                .lastStatus(job.getLastStatus())
                .lastDurationMs(job.getLastDurationMs())
                .lastMessage(job.getLastMessage())
                .nextRunAt(job.getNextRunAt())
                .build();
    }

    private String resolveStatus(SystemJob job, String readinessMessage) {
        if (scheduler.isRunning(job.getJobCode())) {
            return STATUS_RUNNING;
        }
        if (!Boolean.TRUE.equals(job.getEnabled())) {
            return STATUS_DISABLED;
        }
        if (readinessMessage != null && !readinessMessage.isBlank()) {
            return STATUS_NOT_READY;
        }
        return STATUS_ENABLED;
    }

    private SystemJobHistoryDto toHistoryDto(SystemJobHistory history) {
        return SystemJobHistoryDto.builder()
                .id(history.getId())
                .jobCode(history.getJobCode())
                .startedAt(history.getStartedAt())
                .finishedAt(history.getFinishedAt())
                .durationMs(history.getDurationMs())
                .status(history.getStatus())
                .message(history.getMessage())
                .triggeredBy(history.getTriggeredBy())
                .build();
    }
}
