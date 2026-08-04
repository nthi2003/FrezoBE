package com.frezo.qtht.service.impl;

import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.entity.ErpPageView;
import com.frezo.qtht.repository.ErpPageViewRepository;
import com.frezo.qtht.service.UsageAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UsageAnalyticsServiceImpl implements UsageAnalyticsService {

    private static final int ROUTE_MAX = 300;
    private static final int MODULE_MAX = 60;
    private static final int DAYS_MIN = 1;
    private static final int DAYS_MAX = 30;

    private final ErpPageViewRepository pageViewRepository;

    @Override
    @Transactional
    public void trackPageView(String route, String moduleCode) {
        if (!StringUtils.hasText(route)) {
            return;
        }

        String normalizedRoute = truncate(route.trim(), ROUTE_MAX);
        String normalizedModule = moduleCode == null ? null : truncate(moduleCode.trim(), MODULE_MAX);
        if (normalizedModule != null && normalizedModule.isEmpty()) {
            normalizedModule = null;
        }

        String username = null;
        try {
            username = SystemUtils.getCurrentUsername();
        } catch (Exception ignored) {
            // anonymous — vẫn ghi nếu endpoint gọi được
        }

        pageViewRepository.save(ErpPageView.builder()
                .username(username)
                .route(normalizedRoute)
                .moduleCode(normalizedModule)
                .viewedAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> topPageViews(int days) {
        int d = Math.max(DAYS_MIN, Math.min(days, DAYS_MAX));
        LocalDateTime from = LocalDate.now().minusDays(d - 1L).atStartOfDay();

        List<Map<String, Object>> modules = pageViewRepository.topModulesSince(from).stream()
                .map(row -> namedCount("code", row))
                .toList();

        List<Map<String, Object>> routes = pageViewRepository.topRoutesSince(from).stream()
                .map(row -> namedCount("route", row))
                .toList();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("days", d);
        data.put("total", pageViewRepository.countByViewedAtGreaterThanEqual(from));
        data.put("topModules", modules);
        data.put("topRoutes", routes);
        return data;
    }

    private static Map<String, Object> namedCount(String key, Object[] row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(key, row[0] != null ? row[0].toString() : "—");
        m.put("count", row[1] instanceof Number n ? n.longValue() : 0L);
        return m;
    }

    private static String truncate(String value, int max) {
        return value.length() > max ? value.substring(0, max) : value;
    }
}
