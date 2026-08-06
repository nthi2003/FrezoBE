package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.JobPositionRequest;
import com.frezo.qlns.dto.response.CategoryUsageResponse;
import com.frezo.qlns.dto.response.JobPositionResponse;

import java.util.List;

public interface JobPositionService {
    List<JobPositionResponse> list();

    JobPositionResponse getById(String id);

    JobPositionResponse create(JobPositionRequest request);

    JobPositionResponse update(String id, JobPositionRequest request);

    void delete(String id);

    CategoryUsageResponse checkCategoryUsage(String categoryCode);
}
