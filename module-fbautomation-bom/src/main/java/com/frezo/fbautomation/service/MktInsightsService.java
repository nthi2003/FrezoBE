package com.frezo.fbautomation.service;

import java.util.Map;

/** Aggregate marketing insights từ lead / post / affiliate / ads (không gọi Meta Graph API). */
public interface MktInsightsService {
    Map<String, Object> dashboard();
}
