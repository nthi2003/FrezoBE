package com.frezo.crm.service;

import com.frezo.crm.common.DealStatus;
import com.frezo.crm.dto.DealRequest;
import com.frezo.crm.entity.Deal;

import java.util.List;

public interface DealService {
    Deal create(DealRequest req);
    Deal update(String id, DealRequest req);
    void delete(String id);
    Deal get(String id);
    List<Deal> byPipeline(String pipelineId);
    List<Deal> byStatus(DealStatus status);
    List<Deal> byOwner(String owner);
    List<Deal> byCustomer(String customerId);
    Deal moveStage(String dealId, String toStageId);
    Deal markWon(String dealId);
    Deal markLost(String dealId, String reason);
}
