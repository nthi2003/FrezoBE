package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.qlns.dto.request.ManagerScoreRequest;
import com.frezo.qlns.dto.request.PerformanceReviewRequest;
import com.frezo.qlns.dto.response.PerformanceReviewResponse;
import com.frezo.qlns.entity.PerformanceReview;
import com.frezo.qlns.repository.PerformanceReviewRepository;
import com.frezo.qlns.service.PerformanceReviewService;
import com.frezo.qlns.service.impl.OkrScopeResolver;
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
    private final OkrScopeResolver scopeResolver;

    @Override
    public List<PerformanceReviewResponse> list(String cycleId, String personId) {
        String me = currentPerson();
        List<PerformanceReview> list;
        if (cycleId != null) list = reviewRepository.findByCycleIdAndIsDeletedFalse(cycleId);
        else if (personId != null) list = reviewRepository.findByPersonIdAndIsDeletedFalse(personId);
        else list = reviewRepository.findByIsDeletedFalseOrderByCreatedDateDesc();
        return list.stream()
                .filter(r -> canView(me, r))
                .map(this::toDto).toList();
    }

    @Override
    @Transactional
    public PerformanceReviewResponse create(PerformanceReviewRequest req) {
        String me = currentPerson();
        if (req.getPersonId() == null || req.getPersonId().isBlank()) {
            req.setPersonId(me);
        }
        if (!me.equals(req.getPersonId()) && !scopeResolver.isAdmin()
                && !scopeResolver.subordinatePersonIds(me).contains(req.getPersonId())) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Không thể tạo đánh giá ngoài phạm vi quản lý");
        }
        validateScore(req.getSelfScore(), "Điểm tự đánh giá");
        if (req.getManagerPersonId() != null && !req.getManagerPersonId().isBlank()
                && !scopeResolver.isAdmin()
                && !scopeResolver.subordinatePersonIds(req.getManagerPersonId()).contains(req.getPersonId())) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Người đánh giá không quản lý nhân viên này");
        }
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
        String me = currentPerson();
        if (!scopeResolver.isAdmin() && !me.equals(r.getPersonId())) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ nhân viên được gửi tự đánh giá của mình");
        }
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
        String me = currentPerson();
        boolean assigned = me.equals(r.getManagerPersonId());
        boolean actualManager = scopeResolver.subordinatePersonIds(me).contains(r.getPersonId());
        if (!scopeResolver.isAdmin() && (!assigned || !actualManager)) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Chỉ quản lý được chọn mới được xác nhận đánh giá");
        }
        validateScore(req.getManagerScore(), "Điểm quản lý");
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

    private boolean canView(String me, PerformanceReview review) {
        return scopeResolver.isAdmin()
                || me.equals(review.getPersonId())
                || me.equals(review.getManagerPersonId())
                || scopeResolver.subordinatePersonIds(me).contains(review.getPersonId());
    }

    private String currentPerson() {
        return scopeResolver.currentPersonId()
                .orElseThrow(() -> new AppException(CommonErrorCode.FORBIDDEN, "Tài khoản chưa liên kết nhân sự"));
    }

    private void validateScore(Double score, String field) {
        if (score != null && (score < 0 || score > 100)) {
            throw new AppException(CommonErrorCode.VALIDATION_FAILED, field + " phải từ 0 đến 100%");
        }
    }
}
