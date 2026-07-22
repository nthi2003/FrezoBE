package com.frezo.crm.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.crm.common.CrmErrorCode;
import com.frezo.crm.common.DealStatus;
import com.frezo.crm.dto.DealRequest;
import com.frezo.crm.entity.Deal;
import com.frezo.crm.entity.Stage;
import com.frezo.crm.repository.DealRepository;
import com.frezo.crm.repository.StageRepository;
import com.frezo.crm.service.DealService;
import com.frezo.crm.service.PipelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DealServiceImpl implements DealService {

    private final DealRepository dealRepo;
    private final StageRepository stageRepo;
    private final PipelineService pipelineService;

    @Override
    @Transactional
    public Deal create(DealRequest r) {
        String pipelineId = r.getPipelineId() != null ? r.getPipelineId()
                : pipelineService.ensureDefault().getId();
        String stageId = r.getStageId() != null ? r.getStageId()
                : pipelineService.stages(pipelineId).stream().findFirst()
                        .map(Stage::getId).orElseThrow(() ->
                                new AppException(CrmErrorCode.STAGE_NOT_FOUND));
        Deal d = Deal.builder()
                .title(r.getTitle())
                .pipelineId(pipelineId)
                .stageId(stageId)
                .customerId(r.getCustomerId())
                .amount(r.getAmount())
                .currency(r.getCurrency() != null ? r.getCurrency() : "VND")
                .probability(r.getProbability())
                .expectedCloseDate(r.getExpectedCloseDate())
                .status(r.getStatus() != null ? r.getStatus() : DealStatus.OPEN)
                .ownerUsername(r.getOwnerUsername())
                .description(r.getDescription())
                .lostReason(r.getLostReason())
                .build();
        d.setIsDeleted(false);
        return dealRepo.save(d);
    }

    @Override
    @Transactional
    public Deal update(String id, DealRequest r) {
        Deal d = get(id);
        d.setTitle(r.getTitle());
        if (r.getPipelineId() != null) d.setPipelineId(r.getPipelineId());
        if (r.getStageId() != null) d.setStageId(r.getStageId());
        d.setCustomerId(r.getCustomerId());
        d.setAmount(r.getAmount());
        if (r.getCurrency() != null) d.setCurrency(r.getCurrency());
        d.setProbability(r.getProbability());
        d.setExpectedCloseDate(r.getExpectedCloseDate());
        if (r.getStatus() != null) d.setStatus(r.getStatus());
        d.setOwnerUsername(r.getOwnerUsername());
        d.setDescription(r.getDescription());
        d.setLostReason(r.getLostReason());
        return dealRepo.save(d);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Deal d = get(id);
        d.setIsDeleted(true);
        dealRepo.save(d);
    }

    @Override
    @Transactional(readOnly = true)
    public Deal get(String id) {
        return dealRepo.findById(id)
                .orElseThrow(() -> new AppException(CrmErrorCode.DEAL_NOT_FOUND, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deal> byPipeline(String pipelineId) {
        return dealRepo.findByPipelineIdAndIsDeletedFalseOrderByCreatedDateDesc(pipelineId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deal> byStatus(DealStatus status) {
        return dealRepo.findByStatusAndIsDeletedFalseOrderByCreatedDateDesc(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deal> byOwner(String owner) {
        return dealRepo.findByOwnerUsernameAndIsDeletedFalseOrderByCreatedDateDesc(owner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Deal> byCustomer(String customerId) {
        return dealRepo.findByCustomerIdAndIsDeletedFalseOrderByCreatedDateDesc(customerId);
    }

    @Override
    @Transactional
    public Deal moveStage(String dealId, String toStageId) {
        Deal d = get(dealId);
        Stage s = stageRepo.findById(toStageId)
                .orElseThrow(() -> new AppException(CrmErrorCode.STAGE_NOT_FOUND, toStageId));
        d.setStageId(s.getId());
        if (s.getProbability() != null) d.setProbability(s.getProbability());
        if (Boolean.TRUE.equals(s.getWon())) {
            d.setStatus(DealStatus.WON);
            d.setClosedDate(LocalDate.now());
        } else if (Boolean.FALSE.equals(s.getWon())) {
            d.setStatus(DealStatus.LOST);
            d.setClosedDate(LocalDate.now());
        } else {
            d.setStatus(DealStatus.OPEN);
        }
        return dealRepo.save(d);
    }

    @Override
    @Transactional
    public Deal markWon(String dealId) {
        Deal d = get(dealId);
        d.setStatus(DealStatus.WON);
        d.setClosedDate(LocalDate.now());
        d.setProbability(100);
        return dealRepo.save(d);
    }

    @Override
    @Transactional
    public Deal markLost(String dealId, String reason) {
        Deal d = get(dealId);
        d.setStatus(DealStatus.LOST);
        d.setClosedDate(LocalDate.now());
        d.setLostReason(reason);
        d.setProbability(0);
        return dealRepo.save(d);
    }
}
