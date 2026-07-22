package com.frezo.approval.service;

import com.frezo.approval.dto.ApprovalRequestDto;
import com.frezo.approval.dto.ApprovalStepDto;
import com.frezo.approval.dto.CreateApprovalRequest;
import com.frezo.common.response.FePage;

import java.util.List;

public interface ApprovalService {

    FePage<ApprovalRequestDto> listMy(String status);

    ApprovalRequestDto approve(String id, String comment);

    ApprovalRequestDto reject(String id, String comment);

    List<ApprovalStepDto> timeline(String id);

    List<ApprovalStepDto> timelineBySubject(String subjectType, String subjectId);

    ApprovalRequestDto findBySubject(String subjectType, String subjectId);

    ApprovalRequestDto create(CreateApprovalRequest req);
}
