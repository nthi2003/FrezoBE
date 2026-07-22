package com.frezo.crm.service;

import com.frezo.crm.dto.MeetingRequest;
import com.frezo.crm.dto.MeetingResponse;

import java.util.List;

public interface MeetingService {
    List<MeetingResponse> list(String dealId);
    MeetingResponse get(String id);
    MeetingResponse create(MeetingRequest req);
    MeetingResponse update(String id, MeetingRequest req);
    void delete(String id);
}
