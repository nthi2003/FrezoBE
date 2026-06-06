package com.frezo.qtht.service;

import com.frezo.qtht.entity.ApiLog;

import java.util.Map;
import com.frezo.qtht.dto.request.ApilogFilter;

import com.frezo.qtht.dto.response.ApiLogStatsResponse;

public interface ApiLogService {
    void saveLog(ApiLog log);
    Map<String, Object> all(ApilogFilter filter);
    ApiLogStatsResponse getStats(ApilogFilter filter);
    void deleteLogs(int days);
    ApiLog findById(String id);
    void delete(String id);
}
