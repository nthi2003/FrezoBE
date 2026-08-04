package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.request.LivestreamEventRequest;
import com.frezo.fbautomation.dto.response.LivestreamEventResponse;

import java.util.List;
import java.util.Map;

public interface LivestreamEventService {
    List<LivestreamEventResponse> list(String status);
    LivestreamEventResponse get(String id);
    LivestreamEventResponse create(LivestreamEventRequest req);
    LivestreamEventResponse update(String id, LivestreamEventRequest req);
    void delete(String id);
    LivestreamEventResponse markNotified(String id);
    LivestreamEventResponse updateStatus(String id, String status);
    Map<String, Object> dashboard();
}
