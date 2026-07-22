package com.frezo.approval.service;

import com.frezo.approval.dto.ApprovalFlowDto;
import com.frezo.approval.dto.ApprovalFlowRequest;

import java.util.List;

public interface ApprovalFlowService {
    List<ApprovalFlowDto> list();
    ApprovalFlowDto create(ApprovalFlowRequest req);
    ApprovalFlowDto update(String id, ApprovalFlowRequest req);
}
