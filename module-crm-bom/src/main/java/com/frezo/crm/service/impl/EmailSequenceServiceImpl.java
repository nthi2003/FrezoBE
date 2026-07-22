package com.frezo.crm.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.crm.dto.EmailSequenceEnrollRequest;
import com.frezo.crm.dto.EmailSequenceEnrollmentResponse;
import com.frezo.crm.dto.EmailSequenceRequest;
import com.frezo.crm.dto.EmailSequenceResponse;
import com.frezo.crm.dto.EmailSequenceStepRequest;
import com.frezo.crm.dto.EmailSequenceStepResponse;
import com.frezo.crm.entity.EmailSequence;
import com.frezo.crm.entity.EmailSequenceEnrollment;
import com.frezo.crm.entity.EmailSequenceStep;
import com.frezo.crm.repository.EmailSequenceEnrollmentRepository;
import com.frezo.crm.repository.EmailSequenceRepository;
import com.frezo.crm.repository.EmailSequenceStepRepository;
import com.frezo.crm.repository.LeadRepository;
import com.frezo.crm.service.EmailSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSequenceServiceImpl implements EmailSequenceService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final EmailSequenceRepository sequenceRepository;
    private final EmailSequenceStepRepository stepRepository;
    private final EmailSequenceEnrollmentRepository enrollmentRepository;
    private final LeadRepository leadRepository;

    @Override
    public List<EmailSequenceResponse> list() {
        return sequenceRepository.findByIsDeletedFalseOrderByCreatedDateDesc().stream()
                .map(this::toDto).toList();
    }

    @Override
    @Transactional
    public EmailSequenceResponse create(EmailSequenceRequest req) {
        EmailSequence seq = EmailSequence.builder()
                .name(req.getName())
                .description(req.getDescription())
                .active(req.getActive() == null || req.getActive())
                .build();
        seq.setId(UUID.randomUUID().toString());
        seq = sequenceRepository.save(seq);
        saveSteps(seq.getId(), req.getSteps());
        return toDto(seq);
    }

    @Override
    @Transactional
    public EmailSequenceResponse update(String id, EmailSequenceRequest req) {
        EmailSequence seq = sequenceRepository.findById(id)
                .filter(s -> Boolean.FALSE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Sequence không tồn tại"));
        if (req.getName() != null) seq.setName(req.getName());
        if (req.getDescription() != null) seq.setDescription(req.getDescription());
        if (req.getActive() != null) seq.setActive(req.getActive());
        sequenceRepository.save(seq);
        if (req.getSteps() != null) {
            stepRepository.findBySequenceIdAndIsDeletedFalseOrderByStepOrderAsc(id)
                    .forEach(s -> { s.setIsDeleted(true); stepRepository.save(s); });
            saveSteps(id, req.getSteps());
        }
        return toDto(seq);
    }

    @Override
    @Transactional
    public EmailSequenceEnrollmentResponse enroll(String sequenceId, EmailSequenceEnrollRequest req) {
        sequenceRepository.findById(sequenceId)
                .filter(s -> Boolean.FALSE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Sequence không tồn tại"));
        leadRepository.findById(req.getLeadId())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Lead không tồn tại"));

        EmailSequenceEnrollment e = EmailSequenceEnrollment.builder()
                .sequenceId(sequenceId)
                .leadId(req.getLeadId())
                .currentStepOrder(0)
                .status("ACTIVE")
                .enrolledAt(LocalDateTime.now())
                .build();
        e.setId(UUID.randomUUID().toString());
        return toEnrollDto(enrollmentRepository.save(e));
    }

    @Override
    @Transactional
    public void processDueSteps() {
        List<EmailSequenceEnrollment> active = enrollmentRepository.findByStatusAndIsDeletedFalse("ACTIVE");
        for (EmailSequenceEnrollment e : active) {
            List<EmailSequenceStep> steps = stepRepository
                    .findBySequenceIdAndIsDeletedFalseOrderByStepOrderAsc(e.getSequenceId());
            int nextOrder = (e.getCurrentStepOrder() == null ? 0 : e.getCurrentStepOrder()) + 1;
            EmailSequenceStep next = steps.stream()
                    .filter(s -> s.getStepOrder() != null && s.getStepOrder() == nextOrder)
                    .findFirst().orElse(null);
            if (next == null) {
                e.setStatus("COMPLETED");
                enrollmentRepository.save(e);
                continue;
            }
            // Stub send — chỉ đánh dấu đã gửi bước.
            log.info("[EmailSequence] stub send seq={} lead={} step={} subject={}",
                    e.getSequenceId(), e.getLeadId(), next.getStepOrder(), next.getSubject());
            e.setCurrentStepOrder(next.getStepOrder());
            e.setLastSentAt(LocalDateTime.now());
            if (steps.stream().noneMatch(s -> s.getStepOrder() != null && s.getStepOrder() > nextOrder)) {
                e.setStatus("COMPLETED");
            }
            enrollmentRepository.save(e);
        }
    }

    private void saveSteps(String sequenceId, List<EmailSequenceStepRequest> steps) {
        if (steps == null) return;
        for (EmailSequenceStepRequest r : steps) {
            EmailSequenceStep s = EmailSequenceStep.builder()
                    .sequenceId(sequenceId)
                    .stepOrder(r.getStepOrder())
                    .delayDays(r.getDelayDays() != null ? r.getDelayDays() : 0)
                    .subject(r.getSubject())
                    .bodyHtml(r.getBodyHtml())
                    .build();
            s.setId(UUID.randomUUID().toString());
            stepRepository.save(s);
        }
    }

    private EmailSequenceResponse toDto(EmailSequence seq) {
        List<EmailSequenceStepResponse> steps = stepRepository
                .findBySequenceIdAndIsDeletedFalseOrderByStepOrderAsc(seq.getId())
                .stream()
                .map(s -> EmailSequenceStepResponse.builder()
                        .id(s.getId()).stepOrder(s.getStepOrder()).delayDays(s.getDelayDays())
                        .subject(s.getSubject()).bodyHtml(s.getBodyHtml()).build())
                .toList();
        return EmailSequenceResponse.builder()
                .id(seq.getId()).name(seq.getName()).description(seq.getDescription())
                .active(seq.getActive()).steps(steps).build();
    }

    private EmailSequenceEnrollmentResponse toEnrollDto(EmailSequenceEnrollment e) {
        return EmailSequenceEnrollmentResponse.builder()
                .id(e.getId()).sequenceId(e.getSequenceId()).leadId(e.getLeadId())
                .currentStepOrder(e.getCurrentStepOrder()).status(e.getStatus())
                .enrolledAt(e.getEnrolledAt() != null ? e.getEnrolledAt().format(ISO) : null)
                .lastSentAt(e.getLastSentAt() != null ? e.getLastSentAt().format(ISO) : null)
                .build();
    }
}
