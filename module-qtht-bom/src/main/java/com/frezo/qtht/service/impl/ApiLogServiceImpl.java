package com.frezo.qtht.service.impl;

import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
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

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApiLogServiceImpl implements ApiLogService {

    /// ThiNVQ : Chỉ xử lý duy nhất 1 việc là save log nha
    private final ApiLogRepository apiLogRepository;
    private final ApiLogMapper apiLogMapper;

    @Override
    public void saveLog(ApiLog log) {
        apiLogRepository.save(log);
    }

    @Override
    public Map<String, Object> all(ApilogFilter filter) {
        Specification<ApiLog> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<ApiLog> entities = apiLogRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));
        List<ApiLogResponse> responses = entities.getContent().stream()
                .map(apiLogMapper::toResponse).toList();
        return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), entities, responses);
    }


    @Override
    @Transactional
    public void deleteLogs(int days) {
        java.time.LocalDateTime cutoffDate = java.time.LocalDateTime.now().minusDays(days);
        apiLogRepository.deleteByEffFromAfter(cutoffDate);
    }

    @Override
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
    public ApiLogStatsResponse getStats(ApilogFilter filter) {
        Specification<ApiLog> specification = createSpecification(filter);
        List<ApiLog> allMatch = apiLogRepository.findAll(specification);

        long total = allMatch.size();
        long success = allMatch.stream().filter(l -> l.getStatusCode() != null && l.getStatusCode() < 400).count();
        long failed = total - success;
        double avgDuration = allMatch.stream()
                .filter(l -> l.getDuration() != null)
                .mapToLong(ApiLog::getDuration)
                .average()
                .orElse(0.0);

        return ApiLogStatsResponse.builder()
                .total(total)
                .success(success)
                .failed(failed)
                .avgDuration(Math.round(avgDuration * 10.0) / 10.0)
                .totalTrend(12.5)
                .failedTrend(-5.2)
                .build();
    }

    private Specification<ApiLog> createSpecification (ApilogFilter filter) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (filter.getSearch() != null && !filter.getSearch().isEmpty()) {
                String searchPattern = "%" + filter.getSearch().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("uri")), searchPattern),
                    cb.like(cb.lower(root.get("username")), searchPattern)
                ));
            }

            if (filter.getMethod() != null && !"all".equalsIgnoreCase(filter.getMethod())) {
                predicates.add(cb.equal(root.get("method"), filter.getMethod()));
            }

            if (filter.getStatusCode() != null) {
                predicates.add(cb.equal(root.get("statusCode"), filter.getStatusCode()));
            }

            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("effFrom"), filter.getFromDate()));
            }

            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("effTo"), filter.getToDate()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

}
