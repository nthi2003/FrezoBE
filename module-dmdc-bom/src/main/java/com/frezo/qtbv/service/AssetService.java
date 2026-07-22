package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.AssetAssignRequest;
import com.frezo.qtbv.dto.request.AssetSaveRequest;
import com.frezo.qtbv.dto.request.AssetTransferCreateRequest;
import com.frezo.qtbv.dto.response.AssetAssignmentResponse;
import com.frezo.qtbv.dto.response.AssetResponse;
import com.frezo.qtbv.dto.response.AssetTransferRequestResponse;

import java.util.List;
import java.util.Map;

public interface AssetService {

    Map<String, Object> list(String keyword, String status, String categoryCode, String assignedPersonId,
                             int page, int size);

    AssetResponse getById(String id);

    AssetResponse create(AssetSaveRequest request);

    AssetResponse update(String id, AssetSaveRequest request);

    void delete(String id);

    /** Cấp phát tài sản cho nhân viên. Chuyển status → IN_USE. */
    AssetResponse assign(String id, AssetAssignRequest request);

    /** Thu hồi tài sản. Chuyển status → AVAILABLE. */
    AssetResponse unassign(String id, String note);

    /** Chuyển sang bảo trì. */
    AssetResponse startMaintenance(String id, String note);

    /** Kết thúc bảo trì (option chi phí). */
    AssetResponse endMaintenance(String id, String note, java.math.BigDecimal cost);

    /** Thanh lý (terminal — không thể chuyển tiếp). */
    AssetResponse dispose(String id, String note);

    /** Timeline audit trail. */
    List<AssetAssignmentResponse> history(String assetId);

    /** KPI stats cho card strip. */
    Map<String, Object> stats();

    // ============================================================
    // Workflow ticket — cấp phát / thu hồi qua approval
    // ============================================================

    /**
     * Tạo ticket yêu cầu cấp phát / thu hồi. Ticket ở trạng thái PENDING,
     * không thay đổi trạng thái Asset ngay.
     */
    AssetTransferRequestResponse createTransferRequest(String assetId, AssetTransferCreateRequest req);

    /** Duyệt ticket (chỉ admin/approver). PENDING → APPROVED. */
    AssetTransferRequestResponse approveTransferRequest(String requestId, String note);

    /** Từ chối ticket. PENDING → REJECTED (terminal). */
    AssetTransferRequestResponse rejectTransferRequest(String requestId, String reason);

    /** Huỷ ticket (chỉ requester hoặc admin, chỉ khi PENDING). PENDING → CANCELLED. */
    AssetTransferRequestResponse cancelTransferRequest(String requestId);

    /**
     * Xác nhận đã bàn giao thực tế. APPROVED → HANDED_OVER + cập nhật Asset:
     * ASSIGN → asset.status = IN_USE + assignedPersonId; RETURN → asset.status = AVAILABLE.
     */
    AssetTransferRequestResponse handoverTransferRequest(String requestId, String note);

    /** Danh sách ticket có filter + phân trang. */
    Map<String, Object> listTransferRequests(String status, String requestType, String assetId,
                                             String personId, String keyword, int page, int size);

    /** Chi tiết một ticket. */
    AssetTransferRequestResponse getTransferRequest(String requestId);
}
