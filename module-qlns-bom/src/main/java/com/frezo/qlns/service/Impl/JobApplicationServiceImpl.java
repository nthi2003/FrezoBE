package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.dto.request.JobApplicationRequest;
import com.frezo.qlns.dto.response.JobApplicationResponse;
import com.frezo.qlns.entity.Candidate;
import com.frezo.qlns.entity.JobApplication;
import com.frezo.qlns.entity.Requisition;
import com.frezo.qlns.recruitment.RecruitmentConstants;
import com.frezo.qlns.recruitment.RecruitmentErrorCode;
import com.frezo.qlns.repository.CandidateRepository;
import com.frezo.qlns.repository.JobApplicationRepository;
import com.frezo.qlns.repository.RequisitionRepository;
import com.frezo.qlns.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * CRUD + workflow đơn ứng tuyển.
 * <p>Logic quan trọng:
 * <ul>
 *   <li>Không cho apply mới nếu requisition đã CLOSED / FILLED.</li>
 *   <li>Không cho apply trùng (candidate × requisition đã tồn tại).</li>
 *   <li>Move stage chỉ cho phép theo {@link RecruitmentConstants#STAGE_TRANSITIONS}.</li>
 *   <li>Khi chuyển sang HIRED, nếu tổng HIRED ≥ quantity của requisition → auto FILLED.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository applicationRepository;
    private final RequisitionRepository requisitionRepository;
    private final CandidateRepository candidateRepository;

    @Override
    @Transactional
    public JobApplicationResponse create(JobApplicationRequest req) {
        Requisition requisition = requisitionRepository.findById(req.getRequisitionId())
                .filter(r -> Boolean.FALSE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.REQUISITION_NOT_FOUND, req.getRequisitionId()));

        if (Set.of(RecruitmentConstants.REQ_CLOSED, RecruitmentConstants.REQ_FILLED)
                .contains(requisition.getStatus())) {
            throw new AppException(RecruitmentErrorCode.REQUISITION_CLOSED);
        }

        Candidate candidate = candidateRepository.findById(req.getCandidateId())
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.CANDIDATE_NOT_FOUND, req.getCandidateId()));

        applicationRepository.findByCandidateIdAndRequisitionIdAndIsDeletedFalse(
                candidate.getId(), requisition.getId())
                .ifPresent(existing -> {
                    throw new AppException(RecruitmentErrorCode.APPLICATION_ALREADY_EXISTS);
                });

        JobApplication e = JobApplication.builder()
                .candidateId(candidate.getId())
                .requisitionId(requisition.getId())
                .stage(RecruitmentConstants.STAGE_APPLIED)
                .appliedDate(LocalDate.now())
                .currentAssignee(req.getCurrentAssignee())
                .build();
        return toResponse(applicationRepository.save(e), candidate, requisition);
    }

    @Override
    public List<JobApplicationResponse> list(String requisitionId, String stage) {
        return applicationRepository.findAll().stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsDeleted()))
                .filter(a -> requisitionId == null || requisitionId.equals(a.getRequisitionId()))
                .filter(a -> stage == null || stage.equalsIgnoreCase(a.getStage()))
                .sorted(Comparator.comparing(JobApplication::getAppliedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponseLight)
                .toList();
    }

    @Override
    @Transactional
    public JobApplicationResponse moveStage(String id, String targetStage) {
        JobApplication app = findOrThrow(id);
        Set<String> allowed = RecruitmentConstants.STAGE_TRANSITIONS.get(app.getStage());
        if (allowed == null) {
            throw new AppException(RecruitmentErrorCode.APPLICATION_FINAL_STAGE);
        }
        if (!allowed.contains(targetStage)) {
            throw new AppException(RecruitmentErrorCode.APPLICATION_STAGE_INVALID,
                    app.getStage(), targetStage);
        }
        app.setStage(targetStage);
        JobApplication saved = applicationRepository.save(app);
        if (RecruitmentConstants.STAGE_HIRED.equals(targetStage)) {
            autoCloseRequisitionIfFilled(app.getRequisitionId());
        }
        return toResponseLight(saved);
    }

    @Override
    @Transactional
    public JobApplicationResponse reject(String id, String reason) {
        JobApplication app = findOrThrow(id);
        if (Set.of(RecruitmentConstants.STAGE_HIRED, RecruitmentConstants.STAGE_REJECTED)
                .contains(app.getStage())) {
            throw new AppException(RecruitmentErrorCode.APPLICATION_FINAL_STAGE);
        }
        app.setStage(RecruitmentConstants.STAGE_REJECTED);
        app.setRejectionReason(reason);
        return toResponseLight(applicationRepository.save(app));
    }

    @Override
    public JobApplicationResponse getById(String id) {
        return toResponseLight(findOrThrow(id));
    }

    @Override
    @Transactional
    public JobApplicationResponse markHired(String id) {
        JobApplication app = findOrThrow(id);
        if (RecruitmentConstants.STAGE_HIRED.equals(app.getStage())) {
            return toResponseLight(app);
        }
        if (Set.of(RecruitmentConstants.STAGE_REJECTED).contains(app.getStage())) {
            throw new AppException(RecruitmentErrorCode.APPLICATION_FINAL_STAGE);
        }
        app.setStage(RecruitmentConstants.STAGE_HIRED);
        JobApplication saved = applicationRepository.save(app);
        autoCloseRequisitionIfFilled(saved.getRequisitionId());
        return toResponseLight(saved);
    }

    private void autoCloseRequisitionIfFilled(String requisitionId) {
        Requisition req = requisitionRepository.findById(requisitionId).orElse(null);
        if (req == null || Objects.equals(RecruitmentConstants.REQ_FILLED, req.getStatus())) return;
        long hiredCount = applicationRepository.countByRequisitionIdAndStageAndIsDeletedFalse(
                requisitionId, RecruitmentConstants.STAGE_HIRED);
        if (req.getQuantity() != null && hiredCount >= req.getQuantity()) {
            req.setStatus(RecruitmentConstants.REQ_FILLED);
            req.setCloseDate(LocalDate.now());
            requisitionRepository.save(req);
            log.info("[Recruitment] Requisition {} auto FILLED ({} / {} hired)",
                    req.getId(), hiredCount, req.getQuantity());
        }
    }

    private JobApplication findOrThrow(String id) {
        return applicationRepository.findById(id)
                .filter(a -> Boolean.FALSE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.APPLICATION_NOT_FOUND, id));
    }

    private JobApplicationResponse toResponseLight(JobApplication e) {
        String candidateName = candidateRepository.findById(e.getCandidateId())
                .map(Candidate::getFullName).orElse(null);
        String requisitionTitle = requisitionRepository.findById(e.getRequisitionId())
                .map(Requisition::getTitle).orElse(null);
        return JobApplicationResponse.builder()
                .id(e.getId())
                .candidateId(e.getCandidateId())
                .candidateName(candidateName)
                .requisitionId(e.getRequisitionId())
                .requisitionTitle(requisitionTitle)
                .stage(e.getStage())
                .appliedDate(e.getAppliedDate())
                .currentAssignee(e.getCurrentAssignee())
                .rejectionReason(e.getRejectionReason())
                .build();
    }

    private JobApplicationResponse toResponse(JobApplication e, Candidate candidate, Requisition requisition) {
        return JobApplicationResponse.builder()
                .id(e.getId())
                .candidateId(e.getCandidateId())
                .candidateName(candidate != null ? candidate.getFullName() : null)
                .requisitionId(e.getRequisitionId())
                .requisitionTitle(requisition != null ? requisition.getTitle() : null)
                .stage(e.getStage())
                .appliedDate(e.getAppliedDate())
                .currentAssignee(e.getCurrentAssignee())
                .rejectionReason(e.getRejectionReason())
                .build();
    }
}
