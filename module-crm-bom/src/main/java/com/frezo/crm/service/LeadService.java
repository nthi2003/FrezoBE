package com.frezo.crm.service;

import com.frezo.crm.common.LeadStatus;
import com.frezo.crm.dto.LeadRequest;
import com.frezo.crm.entity.Lead;

import java.util.List;

public interface LeadService {
    Lead create(LeadRequest req);
    Lead update(String id, LeadRequest req);
    void delete(String id);
    Lead get(String id);
    List<Lead> list();
    List<Lead> byStatus(LeadStatus status);
    List<Lead> byOwner(String ownerUsername);

    /**
     * Convert Lead → Deal (tự chọn pipeline mặc định).
     * Trả về dealId vừa tạo.
     */
    String convert(String leadId, String pipelineId, String stageId,
                   String customerId, java.math.BigDecimal amount);
}
