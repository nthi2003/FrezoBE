package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.common.QlnsErrorCode;
import com.frezo.qlns.dto.request.JobPositionRequest;
import com.frezo.qlns.dto.response.CategoryUsageResponse;
import com.frezo.qlns.dto.response.JobPositionResponse;
import com.frezo.qlns.entity.JobPosition;
import com.frezo.qlns.repository.JobPositionRepository;
import com.frezo.qlns.service.JobPositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class JobPositionServiceImpl implements JobPositionService {

    private final JobPositionRepository jobPositionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<JobPositionResponse> list() {
        return jobPositionRepository.findByIsDeletedFalseOrderByOrderIndexAscNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JobPositionResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public JobPositionResponse create(JobPositionRequest request) {
        JobPosition entity = JobPosition.builder()
                .name(request.getName().trim())
                .rankCode(request.getRankCode().trim())
                .titleCode(request.getTitleCode().trim())
                .activated(request.getActivated() != null ? request.getActivated() : true)
                .orderIndex(request.getOrderIndex())
                .build();
        entity.setIsDeleted(false);
        return toResponse(jobPositionRepository.save(entity));
    }

    @Override
    @Transactional
    public JobPositionResponse update(String id, JobPositionRequest request) {
        JobPosition entity = findOrThrow(id);
        if (request.getName() != null) entity.setName(request.getName().trim());
        if (request.getRankCode() != null) entity.setRankCode(request.getRankCode().trim());
        if (request.getTitleCode() != null) entity.setTitleCode(request.getTitleCode().trim());
        if (request.getActivated() != null) entity.setActivated(request.getActivated());
        if (request.getOrderIndex() != null) entity.setOrderIndex(request.getOrderIndex());
        return toResponse(jobPositionRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        JobPosition entity = findOrThrow(id);
        entity.setIsDeleted(true);
        jobPositionRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryUsageResponse checkCategoryUsage(String categoryCode) {
        String code = categoryCode != null ? categoryCode.trim() : "";
        List<JobPosition> byRank = jobPositionRepository.findByIsDeletedFalseAndRankCode(code);
        List<JobPosition> byTitle = jobPositionRepository.findByIsDeletedFalseAndTitleCode(code);
        List<String> names = Stream.concat(byRank.stream(), byTitle.stream())
                .sorted(Comparator.comparing(JobPosition::getName))
                .map(JobPosition::getName)
                .distinct()
                .toList();
        long count = names.size();
        String message = count > 0
                ? "Bạn cần thay đổi vị trí công việc trước khi xóa!"
                : null;
        return CategoryUsageResponse.builder()
                .categoryCode(code)
                .usageCount(count)
                .positionNames(names)
                .message(message)
                .build();
    }

    private JobPosition findOrThrow(String id) {
        return jobPositionRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new AppException(QlnsErrorCode.ENTITY_NOT_FOUND));
    }

    private JobPositionResponse toResponse(JobPosition e) {
        return JobPositionResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .rankCode(e.getRankCode())
                .titleCode(e.getTitleCode())
                .activated(e.getActivated())
                .orderIndex(e.getOrderIndex())
                .build();
    }
}
