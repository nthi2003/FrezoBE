package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.LeaveRequestAddRequest;
import com.frezo.qlns.dto.response.LeaveRequestResponse;

import java.util.List;
import java.util.Map;

public interface LeaveRequestService {
    LeaveRequestResponse create(LeaveRequestAddRequest request);
    LeaveRequestResponse approve(String id);
    LeaveRequestResponse reject(String id, String reason);
    LeaveRequestResponse cancel(String id);
    List<LeaveRequestResponse> getMyRequests(String contractId);
    Map<String, Object> allPending(int page, int size);
}
