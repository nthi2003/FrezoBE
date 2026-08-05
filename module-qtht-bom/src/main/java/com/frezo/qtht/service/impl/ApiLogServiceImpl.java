package com.frezo.qtht.service.impl;

import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.response.PageResponse;
import com.frezo.qtht.dto.request.ApilogFilter;
import com.frezo.qtht.dto.response.ApiLogResponse;
import com.frezo.qtht.dto.response.ApiLogStatsResponse;
import com.frezo.qtht.entity.ApiLog;
import com.frezo.qtht.mapper.ApiLogMapper;
import com.frezo.qtht.repository.ApiLogRepository;
import com.frezo.qtht.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiLogServiceImpl implements ApiLogService {

    private final ApiLogRepository apiLogRepository;
    private final ApiLogMapper apiLogMapper;

    @Override
    @Transactional
    public void saveLog(ApiLog log) {
        apiLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApiLogResponse> all(ApilogFilter filter) {
        Specification<ApiLog> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<ApiLog> entities = apiLogRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));
        return PageResponse.from(entities, apiLogMapper::toResponse);
    }

    @Override
    @Transactional
    public void deleteLogs(int days) {
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(days);
        apiLogRepository.deleteOlderThan(cutoffDate);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiLog findById(String id) {
        return apiLogRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found"));
    }

    @Override
    @Transactional
    public void delete(String id) {
        apiLogRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiLogStatsResponse getStats(ApilogFilter filter) {
        Specification<ApiLog> base = createSpecification(filter);
        long total = apiLogRepository.count(base);

        Specification<ApiLog> successSpec = base.and((root, query, cb) ->
                cb.lessThan(root.<Integer>get("statusCode"), 400));
        long success = apiLogRepository.count(successSpec);
        long failed = Math.max(0, total - success);

        // Avg duration — sample via page of matching rows is expensive; use full stream only when total nhỏ.
        double avgDuration = 0.0;
        if (total > 0 && total <= 5_000) {
            avgDuration = apiLogRepository.findAll(base).stream()
                    .filter(l -> l.getDuration() != null)
                    .mapToLong(ApiLog::getDuration)
                    .average()
                    .orElse(0.0);
        }

        return ApiLogStatsResponse.builder()
                .total(total)
                .success(success)
                .failed(failed)
                .avgDuration(Math.round(avgDuration * 10.0) / 10.0)
                .totalTrend(0)
                .failedTrend(0)
                .build();
    }

    private Specification<ApiLog> createSpecification(ApilogFilter filter) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("uri")), searchPattern),
                        cb.like(cb.lower(root.get("username")), searchPattern),
                        cb.like(cb.lower(root.get("ipAddress")), searchPattern),
                        cb.like(cb.lower(root.get("module")), searchPattern)
                ));
            }

            if (filter.getMethod() != null && !"all".equalsIgnoreCase(filter.getMethod())) {
                predicates.add(cb.equal(root.get("method"), filter.getMethod().toUpperCase()));
            }

            if (filter.getStatusCode() != null) {
                predicates.add(cb.equal(root.get("statusCode"), filter.getStatusCode()));
            }

            applyStatusGroup(filter, root, cb, predicates);

            if (Boolean.TRUE.equals(filter.getErrorsOnly())) {
                predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("statusCode"), 400));
            }

            if (filter.getIpAddress() != null && !filter.getIpAddress().isEmpty()) {
                predicates.add(cb.like(root.get("ipAddress"), "%" + filter.getIpAddress() + "%"));
            }

            if (filter.getUsername() != null && !filter.getUsername().isEmpty()) {
                predicates.add(cb.equal(cb.lower(root.get("username")), filter.getUsername().toLowerCase()));
            }

            if (filter.getUri() != null && !filter.getUri().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("uri")), "%" + filter.getUri().toLowerCase() + "%"));
            }

            if (filter.getModule() != null && !filter.getModule().isEmpty()
                    && !"all".equalsIgnoreCase(filter.getModule())) {
                predicates.add(cb.equal(cb.lower(root.get("module")), filter.getModule().toLowerCase()));
            }

            if (filter.getDurationMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("duration"), filter.getDurationMin()));
            }

            if (filter.getDurationMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("duration"), filter.getDurationMax()));
            }

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("effFrom"), filter.getFromDate()));
            }

            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("effFrom"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private void applyStatusGroup(
            ApilogFilter filter,
            jakarta.persistence.criteria.Root<ApiLog> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            java.util.List<jakarta.persistence.criteria.Predicate> predicates) {
        String group = filter.getStatusGroup();
        if (group == null || group.isBlank() || "all".equalsIgnoreCase(group)) {
            return;
        }
        switch (group.toLowerCase()) {
            case "2xx" -> predicates.add(cb.and(
                    cb.greaterThanOrEqualTo(root.<Integer>get("statusCode"), 200),
                    cb.lessThan(root.<Integer>get("statusCode"), 300)));
            case "3xx" -> predicates.add(cb.and(
                    cb.greaterThanOrEqualTo(root.<Integer>get("statusCode"), 300),
                    cb.lessThan(root.<Integer>get("statusCode"), 400)));
            case "4xx" -> predicates.add(cb.and(
                    cb.greaterThanOrEqualTo(root.<Integer>get("statusCode"), 400),
                    cb.lessThan(root.<Integer>get("statusCode"), 500)));
            case "5xx" -> predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("statusCode"), 500));
            default -> { /* ignore unknown */ }
        }
    }
}
