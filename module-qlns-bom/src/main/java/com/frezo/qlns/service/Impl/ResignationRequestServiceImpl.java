package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.utils.SecureCodeGenerator;
import com.frezo.qlns.common.QlnsErrorCode;
import com.frezo.qlns.dto.request.ResignationApproveRequest;
import com.frezo.qlns.dto.request.ResignationCreateRequest;
import com.frezo.qlns.dto.request.ResignationHandoverRequest;
import com.frezo.qlns.dto.response.ResignationResponse;
import com.frezo.qlns.entity.ResignationRequest;
import com.frezo.qlns.repository.ResignationRequestRepository;
import com.frezo.qlns.service.ResignationRequestService;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResignationRequestServiceImpl implements ResignationRequestService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String STATUS_REQUESTED = "REQUESTED";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_HANDOVER_DONE = "HANDOVER_DONE";
    private static final String STATUS_PAYROLL_SETTLED = "PAYROLL_SETTLED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    private final ResignationRequestRepository repository;
    private final PersonRepository personRepository;
    private final PersonService personService;

    @Override
    @Transactional
    public ResignationResponse create(ResignationCreateRequest request) {
        if (request.getPersonId() == null || request.getPersonId().isBlank()) {
            throw new AppException(QlnsErrorCode.RESIGNATION_PERSON_REQUIRED);
        }
        if (request.getExpectedLastDay() == null) {
            throw new AppException(QlnsErrorCode.RESIGNATION_LAST_DAY_REQUIRED);
        }
        Person person = personRepository.findById(request.getPersonId())
                .filter(p -> Boolean.FALSE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new AppException(QlnsErrorCode.PERSON_NOT_FOUND));
        if (!Boolean.TRUE.equals(person.getActivated())) {
            throw new AppException(QlnsErrorCode.RESIGNATION_PERSON_INACTIVE);
        }

        ResignationRequest entity = ResignationRequest.builder()
                .requestCode(SecureCodeGenerator.generateCode("RES"))
                .personId(person.getId())
                .personName(person.getName())
                .expectedLastDay(request.getExpectedLastDay())
                .reason(request.getReason())
                .status(STATUS_REQUESTED)
                .laptopReturned(false)
                .badgeReturned(false)
                .docsHandedOver(false)
                .build();
        return toDto(repository.save(entity));
    }

    @Override
    public ResignationResponse getById(String id) {
        return toDto(findActive(id));
    }

    @Override
    public List<ResignationResponse> list(String personId, String status) {
        List<ResignationRequest> rows;
        if (personId != null && !personId.isBlank()) {
            rows = repository.findByPersonIdAndIsDeletedFalseOrderByCreatedDateDesc(personId);
        } else if (status != null && !status.isBlank()) {
            rows = repository.findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(status);
        } else {
            rows = repository.findByIsDeletedFalseOrderByCreatedDateDesc();
        }
        return rows.stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public ResignationResponse approve(String id, ResignationApproveRequest request) {
        ResignationRequest entity = findActive(id);
        assertStatus(entity, STATUS_REQUESTED);

        LocalDate actual = request != null && request.getActualLastDay() != null
                ? request.getActualLastDay()
                : entity.getExpectedLastDay();
        if (actual == null) {
            throw new AppException(QlnsErrorCode.RESIGNATION_LAST_DAY_REQUIRED);
        }

        String actor = SystemUtils.getCurrentUsername();
        LocalDateTime now = LocalDateTime.now();
        entity.setActualLastDay(actual);
        entity.setStatus(STATUS_APPROVED);
        entity.setManagerApprovedBy(actor);
        entity.setManagerApprovedAt(now);
        entity.setHrConfirmedBy(actor);
        entity.setHrConfirmedAt(now);
        return toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ResignationResponse confirmHandover(String id, ResignationHandoverRequest request) {
        ResignationRequest entity = findActive(id);
        assertStatus(entity, STATUS_APPROVED);

        if (request != null) {
            if (request.getLaptopReturned() != null) {
                entity.setLaptopReturned(request.getLaptopReturned());
            }
            if (request.getBadgeReturned() != null) {
                entity.setBadgeReturned(request.getBadgeReturned());
            }
            if (request.getDocsHandedOver() != null) {
                entity.setDocsHandedOver(request.getDocsHandedOver());
            }
            if (request.getNote() != null) {
                entity.setHandoverNote(request.getNote());
            }
        }

        boolean allDone = Boolean.TRUE.equals(entity.getLaptopReturned())
                && Boolean.TRUE.equals(entity.getBadgeReturned())
                && Boolean.TRUE.equals(entity.getDocsHandedOver());
        if (!allDone) {
            throw new AppException(QlnsErrorCode.RESIGNATION_HANDOVER_INCOMPLETE);
        }

        entity.setStatus(STATUS_HANDOVER_DONE);
        entity.setHandoverAt(LocalDateTime.now());
        return toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ResignationResponse settlePayroll(String id) {
        ResignationRequest entity = findActive(id);
        assertStatus(entity, STATUS_HANDOVER_DONE);

        entity.setStatus(STATUS_PAYROLL_SETTLED);
        entity.setPayrollSettledAt(LocalDateTime.now());
        return toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ResignationResponse complete(String id) {
        ResignationRequest entity = findActive(id);
        assertStatus(entity, STATUS_PAYROLL_SETTLED);

        LocalDateTime now = LocalDateTime.now();
        entity.setStatus(STATUS_COMPLETED);
        entity.setUserRevokedAt(now);
        entity.setCompletedAt(now);

        Person person = personRepository.findById(entity.getPersonId()).orElse(null);
        if (person != null && Boolean.TRUE.equals(person.getActivated())) {
            personService.deactivate(entity.getPersonId());
        }

        return toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ResignationResponse cancel(String id) {
        ResignationRequest entity = findActive(id);
        if (STATUS_COMPLETED.equals(entity.getStatus()) || STATUS_CANCELLED.equals(entity.getStatus())) {
            throw new AppException(QlnsErrorCode.RESIGNATION_INVALID_STATUS);
        }
        entity.setStatus(STATUS_CANCELLED);
        return toDto(repository.save(entity));
    }

    private ResignationRequest findActive(String id) {
        return repository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(QlnsErrorCode.RESIGNATION_NOT_FOUND));
    }

    private void assertStatus(ResignationRequest entity, String expected) {
        if (!expected.equals(entity.getStatus())) {
            throw new AppException(QlnsErrorCode.RESIGNATION_INVALID_STATUS);
        }
    }

    private ResignationResponse toDto(ResignationRequest e) {
        return ResignationResponse.builder()
                .id(e.getId())
                .requestCode(e.getRequestCode())
                .personId(e.getPersonId())
                .personName(e.getPersonName())
                .expectedLastDay(formatDate(e.getExpectedLastDay()))
                .actualLastDay(formatDate(e.getActualLastDay()))
                .reason(e.getReason())
                .status(e.getStatus())
                .managerApprovedBy(e.getManagerApprovedBy())
                .managerApprovedAt(formatTs(e.getManagerApprovedAt()))
                .hrConfirmedBy(e.getHrConfirmedBy())
                .hrConfirmedAt(formatTs(e.getHrConfirmedAt()))
                .laptopReturned(e.getLaptopReturned())
                .badgeReturned(e.getBadgeReturned())
                .docsHandedOver(e.getDocsHandedOver())
                .handoverNote(e.getHandoverNote())
                .handoverAt(formatTs(e.getHandoverAt()))
                .payrollSettledAt(formatTs(e.getPayrollSettledAt()))
                .userRevokedAt(formatTs(e.getUserRevokedAt()))
                .completedAt(formatTs(e.getCompletedAt()))
                .createdDate(formatTs(e.getCreatedDate()))
                .createdBy(e.getCreatedBy())
                .build();
    }

    private String formatDate(LocalDate d) {
        return d != null ? d.format(DATE) : null;
    }

    private String formatTs(LocalDateTime t) {
        return t != null ? t.format(ISO) : null;
    }
}
