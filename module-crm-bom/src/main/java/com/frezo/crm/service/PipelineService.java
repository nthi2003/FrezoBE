package com.frezo.crm.service;

import com.frezo.crm.dto.PipelineRequest;
import com.frezo.crm.entity.Pipeline;
import com.frezo.crm.entity.Stage;

import java.util.List;

public interface PipelineService {
    Pipeline create(PipelineRequest req);
    Pipeline update(String id, PipelineRequest req);
    void delete(String id);
    Pipeline get(String id);
    List<Pipeline> list();
    List<Stage> stages(String pipelineId);

    /** Đảm bảo có ít nhất 1 pipeline mặc định + 5 stage chuẩn. Idempotent. */
    Pipeline ensureDefault();
}
