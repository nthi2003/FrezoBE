package com.frezo.qlns.service;

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
     * Callback dùng nội bộ khi Offer được ACCEPTED — chuyển thẳng sang HIRED
     * mà không cần đi qua {@link #moveStage(String, String)} để bảo toàn transition
     * với validator riêng ({@code OFFER → HIRED} thường bị chặn bởi UI, nhưng flow này hợp lệ).
     */
    JobApplicationResponse markHired(String id);
}
