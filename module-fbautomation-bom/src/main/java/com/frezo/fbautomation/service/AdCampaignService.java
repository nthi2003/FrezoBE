package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.request.AdCampaignRequest;
import com.frezo.fbautomation.dto.response.AdCampaignResponse;

import java.util.List;
import java.util.Map;

public interface AdCampaignService {
    List<AdCampaignResponse> list(String platform, String status);
    AdCampaignResponse get(String id);
    AdCampaignResponse create(AdCampaignRequest req);
    AdCampaignResponse update(String id, AdCampaignRequest req);
    void delete(String id);
    Map<String, Object> dashboard();
}
