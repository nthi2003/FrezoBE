package com.frezo.qtht.service;

import java.util.Map;

public interface UsageAnalyticsService {

    /** Ghi pageview (route trống → no-op). */
    void trackPageView(String route, String moduleCode);

    /** Top module/route trong N ngày gần nhất (1–30). */
    Map<String, Object> topPageViews(int days);
}
