package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.dto.request.RequisitionRequest;
import com.frezo.qlns.dto.response.RequisitionResponse;
import com.frezo.qlns.entity.Requisition;
import com.frezo.qlns.recruitment.RecruitmentConstants;
import com.frezo.qlns.recruitment.RecruitmentErrorCode;
import com.frezo.qlns.repository.JobApplicationRepository;
import com.frezo.qlns.repository.RequisitionRepository;
import com.frezo.qlns.service.RequisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * CRUD Requisition + đóng nhu cầu tuyển. Đếm HIRED để FE biết còn slot không.
 */
@Service
@RequiredArgsConstructor
public class RequisitionServiceImpl implements RequisitionService {

    private final RequisitionRepository requisitionRepository;
    private final JobApplicationRepository jobApplicationRepository;

    @Override
    @Transactional
    public RequisitionResponse create(RequisitionRequest req) {
        Requisition e = Requisition.builder()
                .title(req.getTitle())
                .departmentId(req.getDepartmentId())
                .quantity(req.getQuantity() != null ? req.getQuantity() : 1)
                .level(req.getLevel())
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .status(RecruitmentConstants.REQ_OPEN)
                .hiringManagerUsername(req.getHiringManagerUsername())
                .openDate(req.getOpenDate() != null ? req.getOpenDate() : LocalDate.now())
                .closeDate(req.getCloseDate())
                .description(req.getDescription())
                .requirements(req.getRequirements())
                .build();
        return toResponse(requisitionRepository.save(e));
    }

    @Override
    @Transactional
    public RequisitionResponse update(String id, RequisitionRequest req) {
        Requisition e = findOrThrow(id);
        if (req.getTitle() != null) e.setTitle(req.getTitle());
        if (req.getDepartmentId() != null) e.setDepartmentId(req.getDepartmentId());
        if (req.getQuantity() != null) e.setQuantity(req.getQuantity());
        if (req.getLevel() != null) e.setLevel(req.getLevel());
        if (req.getMinSalary() != null) e.setMinSalary(req.getMinSalary());
        if (req.getMaxSalary() != null) e.setMaxSalary(req.getMaxSalary());
        if (req.getHiringManagerUsername() != null) e.setHiringManagerUsername(req.getHiringManagerUsername());
        if (req.getOpenDate() != null) e.setOpenDate(req.getOpenDate());
        if (req.getCloseDate() != null) e.setCloseDate(req.getCloseDate());
        if (req.getDescription() != null) e.setDescription(req.getDescription());
        if (req.getRequirements() != null) e.setRequirements(req.getRequirements());
        if (req.getStatus() != null) e.setStatus(req.getStatus());
        return toResponse(requisitionRepository.save(e));
    }

    @Override
    public RequisitionResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    public List<RequisitionResponse> list(String status) {
        return requisitionRepository.findAll().stream()
                .filter(r -> Boolean.FALSE.equals(r.getIsDeleted()))
                .filter(r -> status == null || status.equalsIgnoreCase(r.getStatus()))
                .sorted(Comparator.comparing(Requisition::getCreatedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RequisitionResponse close(String id) {
        Requisition e = findOrThrow(id);
        e.setStatus(RecruitmentConstants.REQ_CLOSED);
        e.setCloseDate(LocalDate.now());
        return toResponse(requisitionRepository.save(e));
    }

    private Requisition findOrThrow(String id) {
        return requisitionRepository.findById(id)
                .filter(r -> Boolean.FALSE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.REQUISITION_NOT_FOUND, id));
    }

    private RequisitionResponse toResponse(Requisition e) {
        long hired = jobApplicationRepository.countByRequisitionIdAndStageAndIsDeletedFalse(
                e.getId(), RecruitmentConstants.STAGE_HIRED);
        return RequisitionResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .departmentId(e.getDepartmentId())
                .quantity(e.getQuantity())
                .level(e.getLevel())
                .minSalary(e.getMinSalary())
                .maxSalary(e.getMaxSalary())
                .status(e.getStatus())
                .hiringManagerUsername(e.getHiringManagerUsername())
                .openDate(e.getOpenDate())
                .closeDate(e.getCloseDate())
                .description(e.getDescription())
                .requirements(e.getRequirements())
                .hiredCount(hired)
                .build();
    }
}
