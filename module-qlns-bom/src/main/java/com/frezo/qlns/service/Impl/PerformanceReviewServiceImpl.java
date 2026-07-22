package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.qlns.dto.request.ManagerScoreRequest;
import com.frezo.qlns.dto.request.PerformanceReviewRequest;
import com.frezo.qlns.dto.response.PerformanceReviewResponse;
import com.frezo.qlns.entity.PerformanceReview;
import com.frezo.qlns.repository.PerformanceReviewRepository;
import com.frezo.qlns.service.PerformanceReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PerformanceReviewServiceImpl implements PerformanceReviewService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final PerformanceReviewRepository reviewRepository;

    @Override
    public List<PerformanceReviewResponse> list(String cycleId, String personId) {
        List<PerformanceReview> list;
        if (cycleId != null) list = reviewRepository.findByCycleIdAndIsDeletedFalse(cycleId);
        else if (personId != null) list = reviewRepository.findByPersonIdAndIsDeletedFalse(personId);
        else list = reviewRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
        return list.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public PerformanceReviewResponse create(PerformanceReviewRequest req) {
        PerformanceReview r = PerformanceReview.builder()
                .cycleId(req.getCycleId())
                .personId(req.getPersonId())
                .managerPersonId(req.getManagerPersonId())
                .selfScore(req.getSelfScore())
                .selfComment(req.getSelfComment())
                .status("DRAFT")
                .build();
        r.setId(UUID.randomUUID().toString());
        return toDto(reviewRepository.save(r));
    }

    @Override
    @Transactional
    public PerformanceReviewResponse submit(String id) {
        PerformanceReview r = find(id);
        if (!"DRAFT".equals(r.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Chỉ DRAFT mới submit được");
        }
        r.setStatus("SUBMITTED");
        r.setSubmittedAt(LocalDateTime.now());
        return toDto(reviewRepository.save(r));
    }

    @Override
    @Transactional
    public PerformanceReviewResponse managerScore(String id, ManagerScoreRequest req) {
        PerformanceReview r = find(id);
        if (!"SUBMITTED".equals(r.getStatus()) && !"SCORED".equals(r.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Review chưa submit");
        }
        r.setManagerScore(req.getManagerScore());
        r.setManagerComment(req.getManagerComment());
        r.setStatus("SCORED");
        r.setScoredAt(LocalDateTime.now());
        return toDto(reviewRepository.save(r));
    }

    private PerformanceReview find(String id) {
        return reviewRepository.findById(id)
                .filter(r -> Boolean.FALSE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Review không tồn tại"));
    }

    private PerformanceReviewResponse toDto(PerformanceReview r) {
        return PerformanceReviewResponse.builder()
                .id(r.getId())
                .cycleId(r.getCycleId())
                .personId(r.getPersonId())
                .managerPersonId(r.getManagerPersonId())
                .selfScore(r.getSelfScore())
                .managerScore(r.getManagerScore())
                .selfComment(r.getSelfComment())
                .managerComment(r.getManagerComment())
                .status(r.getStatus())
                .submittedAt(r.getSubmittedAt() != null ? r.getSubmittedAt().format(ISO) : null)
                .scoredAt(r.getScoredAt() != null ? r.getScoredAt().format(ISO) : null)
                .build();
    }
}
