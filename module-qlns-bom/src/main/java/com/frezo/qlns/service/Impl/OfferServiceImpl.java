package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.dto.request.OfferRequest;
import com.frezo.qlns.dto.response.OfferResponse;
import com.frezo.qlns.entity.JobApplication;
import com.frezo.qlns.entity.Offer;
import com.frezo.qlns.recruitment.RecruitmentConstants;
import com.frezo.qlns.recruitment.RecruitmentErrorCode;
import com.frezo.qlns.repository.JobApplicationRepository;
import com.frezo.qlns.repository.OfferRepository;
import com.frezo.qlns.service.JobApplicationService;
import com.frezo.qlns.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Quản lý Offer + workflow send/accept/reject. Khi Accept tự chuyển {@link JobApplication}
 * sang HIRED thông qua {@link JobApplicationService#markHired(String)} (sẽ auto-close requisition
 * nếu đủ quantity).
 */
@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobApplicationService jobApplicationService;

    @Override
    @Transactional
    public OfferResponse create(OfferRequest req) {
        JobApplication app = applicationRepository.findById(req.getApplicationId())
                .filter(a -> Boolean.FALSE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.APPLICATION_NOT_FOUND, req.getApplicationId()));

        Offer e = Offer.builder()
                .applicationId(app.getId())
                .offeredSalary(req.getOfferedSalary())
                .startDate(req.getStartDate())
                .expiresAt(req.getExpiresAt())
                .status(RecruitmentConstants.OFFER_DRAFT)
                .notes(req.getNotes())
                .build();
        return toResponse(offerRepository.save(e));
    }

    @Override
    @Transactional
    public OfferResponse send(String id) {
        Offer e = requireStatus(id, Set.of(RecruitmentConstants.OFFER_DRAFT));
        e.setStatus(RecruitmentConstants.OFFER_SENT);
        e.setSentAt(LocalDateTime.now());
        return toResponse(offerRepository.save(e));
    }

    @Override
    @Transactional
    public OfferResponse accept(String id) {
        Offer e = requireStatus(id, Set.of(RecruitmentConstants.OFFER_SENT, RecruitmentConstants.OFFER_DRAFT));
        e.setStatus(RecruitmentConstants.OFFER_ACCEPTED);
        e.setRespondedAt(LocalDateTime.now());
        Offer saved = offerRepository.save(e);
        // Auto chuyển Application → HIRED và (nếu đủ quantity) close Requisition.
        jobApplicationService.markHired(saved.getApplicationId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public OfferResponse reject(String id) {
        Offer e = requireStatus(id, Set.of(RecruitmentConstants.OFFER_SENT, RecruitmentConstants.OFFER_DRAFT));
        e.setStatus(RecruitmentConstants.OFFER_REJECTED);
        e.setRespondedAt(LocalDateTime.now());
        return toResponse(offerRepository.save(e));
    }

    private Offer requireStatus(String id, Set<String> allowed) {
        Offer e = offerRepository.findById(id)
                .filter(o -> Boolean.FALSE.equals(o.getIsDeleted()))
                .orElseThrow(() -> new AppException(RecruitmentErrorCode.OFFER_NOT_FOUND, id));
        if (!allowed.contains(e.getStatus())) {
            throw new AppException(RecruitmentErrorCode.OFFER_STATUS_INVALID, e.getStatus());
        }
        return e;
    }

    private OfferResponse toResponse(Offer e) {
        return OfferResponse.builder()
                .id(e.getId())
                .applicationId(e.getApplicationId())
                .offeredSalary(e.getOfferedSalary())
                .startDate(e.getStartDate())
                .expiresAt(e.getExpiresAt())
                .status(e.getStatus())
                .notes(e.getNotes())
                .sentAt(e.getSentAt())
                .respondedAt(e.getRespondedAt())
                .build();
    }
}
