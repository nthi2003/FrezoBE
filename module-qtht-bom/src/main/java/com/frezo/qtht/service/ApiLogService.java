package com.frezo.qtht.service;

import com.frezo.common.response.PageResponse;
import com.frezo.qtht.entity.ApiLog;
import com.frezo.qtht.dto.request.ApilogFilter;
import com.frezo.qtht.dto.response.ApiLogResponse;
import com.frezo.qtht.dto.response.ApiLogStatsResponse;

public interface ApiLogService {
    void saveLog(ApiLog log);
    PageResponse<ApiLogResponse> all(ApilogFilter filter);
    ApiLogStatsResponse getStats(ApilogFilter filter);
    void deleteLogs(int days);
    ApiLog findById(String id);
    void delete(String id);
}
