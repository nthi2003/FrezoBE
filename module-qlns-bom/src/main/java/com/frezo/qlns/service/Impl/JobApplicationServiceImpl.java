package com.frezo.qlns.service.Impl;

import com.frezo.auth.dto.request.RegisterRequest;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.QTHTException;
import com.frezo.qlns.dto.request.HireRequest;
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
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.UserAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * CRUD + workflow đơn ứng tuyển.
 * <p>LNK-06: khi {@code qlns.recruitment.hire.require-user-account=true} (policy A),
 * hire phải kèm User+Role; idempotent nếu username đã có.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {

    private static final String APP_CODE = "QTHT";
    private static final String ERR_USER_ROLE_EXISTS = "exception.userRole.exists";

    private final JobApplicationRepository applicationRepository;
    private final RequisitionRepository requisitionRepository;
    private final CandidateRepository candidateRepository;
    private final UserAdminService userAdminService;
    private final UserRepository userRepository;
    private final PersonRepository personRepository;

    /** LNK-06 policy A (default) = bắt buộc User+Role khi hire. */
    @Value("${qlns.recruitment.hire.require-user-account:true}")
    private boolean requireUserAccount;

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
        if (RecruitmentConstants.STAGE_HIRED.equals(targetStage) && requireUserAccount) {
            throw new AppException(RecruitmentErrorCode.HIRE_USER_REQUIRED);
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
        return markHired(id, null);
    }

    @Override
    @Transactional
    public JobApplicationResponse markHired(String id, HireRequest hireRequest) {
        JobApplication app = findOrThrow(id);
        if (RecruitmentConstants.STAGE_HIRED.equals(app.getStage())) {
            return toResponseLight(app);
        }
        if (Set.of(RecruitmentConstants.STAGE_REJECTED).contains(app.getStage())) {
            throw new AppException(RecruitmentErrorCode.APPLICATION_FINAL_STAGE);
        }

        if (requireUserAccount) {
            ensureHireUserAccount(app, hireRequest);
        }

        app.setStage(RecruitmentConstants.STAGE_HIRED);
        JobApplication saved = applicationRepository.save(app);
        autoCloseRequisitionIfFilled(saved.getRequisitionId());
        return toResponseLight(saved);
    }

    /**
     * Policy A: validate + tạo/gán User+Role. Idempotent nếu username đã tồn tại.
     */
    private void ensureHireUserAccount(JobApplication app, HireRequest hire) {
        if (hire == null
                || !StringUtils.hasText(hire.getUsername())
                || !StringUtils.hasText(hire.getPassword())) {
            throw new AppException(RecruitmentErrorCode.HIRE_USER_REQUIRED);
        }
        if (!StringUtils.hasText(hire.getRoleCode())) {
            throw new AppException(RecruitmentErrorCode.HIRE_ROLE_REQUIRED);
        }

        Candidate candidate = candidateRepository.findById(app.getCandidateId())
                .filter(c -> Boolean.FALSE.equals(c.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.CANDIDATE_NOT_FOUND, app.getCandidateId()));

        String username = hire.getUsername().trim();
        String roleCode = hire.getRoleCode().trim();

        if (userRepository.findByUserName(username).isPresent()) {
            try {
                userAdminService.assignRole(username, roleCode, APP_CODE);
            } catch (QTHTException ex) {
                if (!ERR_USER_ROLE_EXISTS.equals(ex.getMessage())) {
                    throw ex;
                }
            }
            log.info("[Recruitment] Hire idempotent — reuse user '{}' + role {}", username, roleCode);
            return;
        }

        RegisterRequest reg = new RegisterRequest();
        reg.setUsername(username);
        reg.setPassword(hire.getPassword());
        reg.setEmail(candidate.getEmail());
        reg.setFullname(candidate.getFullName());
        reg.setDataAction((short) 1);
        reg.setRoleId(roleCode);

        if (StringUtils.hasText(candidate.getEmail())) {
            personRepository.findByEmail(candidate.getEmail()).ifPresent(p -> reg.setPersonId(p.getId()));
        }

        userAdminService.register(reg);
        log.info("[Recruitment] Hire created user '{}' role {} for candidate {}",
                username, roleCode, candidate.getId());
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
