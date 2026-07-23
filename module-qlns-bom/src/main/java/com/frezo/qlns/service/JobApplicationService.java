package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.HireRequest;
import com.frezo.qlns.dto.request.JobApplicationRequest;
import com.frezo.qlns.dto.response.JobApplicationResponse;

import java.util.List;

public interface JobApplicationService {

    JobApplicationResponse create(JobApplicationRequest req);

    List<JobApplicationResponse> list(String requisitionId, String stage);

    JobApplicationResponse moveStage(String id, String targetStage);

    JobApplicationResponse reject(String id, String reason);

    JobApplicationResponse getById(String id);

    /**
     * Hire không kèm account — chỉ hợp lệ khi policy B
     * ({@code qlns.recruitment.hire.require-user-account=false}) hoặc đã HIRED (idempotent).
     */
    JobApplicationResponse markHired(String id);

    /**
     * LNK-06 policy A: hire + tạo User+Role (idempotent nếu username đã tồn tại).
     */
    JobApplicationResponse markHired(String id, HireRequest hireRequest);
}
