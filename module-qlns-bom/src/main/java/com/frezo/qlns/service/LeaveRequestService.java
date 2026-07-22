package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.LeaveRequestAddRequest;
import com.frezo.qlns.dto.response.LeaveRequestHistoryResponse;
import com.frezo.qlns.dto.response.LeaveRequestResponse;

import java.util.List;
import java.util.Map;

public interface LeaveRequestService {

    LeaveRequestResponse create(LeaveRequestAddRequest request);

    /**
     * Duyệt đơn — tự động phát hiện cấp (Manager hoặc HR) dựa trên
     * {@code entity.status + current user}. Xem
     * {@link com.frezo.qlns.service.impl.LeaveRequestServiceImpl} javadoc state machine.
     */
    LeaveRequestResponse approve(String id);

    LeaveRequestResponse reject(String id, String reason);

    LeaveRequestResponse cancel(String id);

    /** Đơn của tôi — tất cả trạng thái, sort mới nhất trước. */
    List<LeaveRequestResponse> getMyRequests(String contractId);

    /**
     * Đơn "cần tôi duyệt" — server tự lọc theo role của current user:
     * admin thấy tất cả, HR thấy PENDING_HR, user thường chỉ thấy đơn mà mình là manager.
     */
    Map<String, Object> allPending(int page, int size);

    /** Timeline audit trail — dùng cho drawer chi tiết. */
    List<LeaveRequestHistoryResponse> getHistory(String requestId);
}
