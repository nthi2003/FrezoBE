package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.ManagerScoreRequest;
import com.frezo.qlns.dto.request.PerformanceReviewRequest;
import com.frezo.qlns.dto.response.PerformanceReviewResponse;

import java.util.List;

public interface PerformanceReviewService {
    List<PerformanceReviewResponse> list(String cycleId, String personId);
    PerformanceReviewResponse create(PerformanceReviewRequest req);
    PerformanceReviewResponse submit(String id);
    PerformanceReviewResponse managerScore(String id, ManagerScoreRequest req);
}
