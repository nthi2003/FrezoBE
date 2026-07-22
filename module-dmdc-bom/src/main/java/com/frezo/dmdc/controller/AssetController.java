package com.frezo.dmdc.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtbv.dto.request.AssetAssignRequest;
import com.frezo.qtbv.dto.request.AssetSaveRequest;
import com.frezo.qtbv.dto.request.AssetTransferCreateRequest;
import com.frezo.qtbv.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * REST cho Quản lý tài sản.
 * <p>
 * Mapping cả {@code /qlts/asset} lẫn {@code /qlts/assets} để backward-compat với FE.
 */
@RestController
@RequestMapping({"/qlts/asset", "/qlts/assets"})
@RequiredArgsConstructor
@Tag(name = "12. Quản lý tài sản (QLTS)", description = "CRUD + workflow cấp phát / bảo trì / thanh lý")
public class AssetController {

    private final AssetService assetService;

    // ---- Query ----

    @Operation(summary = "Danh sách tài sản (có filter + phân trang)")
    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String categoryCode,
                               @RequestParam(required = false) String assignedPersonId,
                               @RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(assetService.list(keyword, status, categoryCode, assignedPersonId, page, size));
    }

    @Operation(summary = "Chi tiết 1 tài sản")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(assetService.getById(id));
    }

    @Operation(summary = "Timeline audit trail của 1 tài sản")
    @GetMapping("/{id}/history")
    public ApiResponse<?> history(@PathVariable String id) {
        return ApiResponse.success(assetService.history(id));
    }

    @Operation(summary = "KPI stats cho card strip")
    @GetMapping("/stats")
    public ApiResponse<?> stats() {
        return ApiResponse.success(assetService.stats());
    }

    // ---- CRUD ----

    @Operation(summary = "Tạo tài sản mới")
    @PostMapping
    public ApiResponse<?> create(@RequestBody AssetSaveRequest request) {
        return ApiResponse.success(assetService.create(request));
    }

    @Operation(summary = "Cập nhật thông tin tài sản (không đổi code, status)")
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody AssetSaveRequest request) {
        return ApiResponse.success(assetService.update(id, request));
    }

    @Operation(summary = "Xoá mềm tài sản (chặn nếu đang cấp phát)")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        assetService.delete(id);
        return ApiResponse.success(null);
    }

    // ---- Workflow transitions ----

    @Operation(summary = "Cấp phát tài sản cho nhân viên")
    @PostMapping("/{id}/assign")
    public ApiResponse<?> assign(@PathVariable String id, @RequestBody AssetAssignRequest request) {
        return ApiResponse.success(assetService.assign(id, request));
    }

    @Operation(summary = "Thu hồi tài sản — trở về AVAILABLE")
    @PostMapping("/{id}/unassign")
    public ApiResponse<?> unassign(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.unassign(id, note));
    }

    @Operation(summary = "Đưa vào bảo trì")
    @PostMapping("/{id}/maintenance/start")
    public ApiResponse<?> startMaintenance(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.startMaintenance(id, note));
    }

    @Operation(summary = "Kết thúc bảo trì (option chi phí)")
    @PostMapping("/{id}/maintenance/end")
    public ApiResponse<?> endMaintenance(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        String note = body != null ? (String) body.getOrDefault("note", null) : null;
        BigDecimal cost = null;
        if (body != null && body.get("cost") != null) {
            try { cost = new BigDecimal(body.get("cost").toString()); }
            catch (NumberFormatException ignored) { }
        }
        return ApiResponse.success(assetService.endMaintenance(id, note, cost));
    }

    @Operation(summary = "Thanh lý (terminal)")
    @PostMapping("/{id}/dispose")
    public ApiResponse<?> dispose(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.dispose(id, note));
    }

    // ============================================================
    // Workflow ticket — cấp phát / thu hồi qua approval
    // ------------------------------------------------------------
    // Đây là API primary — thay cho /assign trực tiếp (vẫn giữ /assign
    // cho admin quick action nội bộ, nhưng UI mặc định đi qua workflow).
    // ============================================================

    @Operation(summary = "Tạo ticket cấp phát / thu hồi tài sản (PENDING)")
    @PostMapping("/{id}/transfer-requests")
    public ApiResponse<?> createTransferRequest(@PathVariable String id,
                                                @RequestBody AssetTransferCreateRequest request) {
        return ApiResponse.success(assetService.createTransferRequest(id, request));
    }

    @Operation(summary = "Danh sách ticket workflow")
    @GetMapping("/transfer-requests")
    public ApiResponse<?> listTransferRequests(@RequestParam(required = false) String status,
                                               @RequestParam(required = false) String requestType,
                                               @RequestParam(required = false) String assetId,
                                               @RequestParam(required = false) String personId,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(assetService.listTransferRequests(
                status, requestType, assetId, personId, keyword, page, size));
    }

    @Operation(summary = "Chi tiết 1 ticket")
    @GetMapping("/transfer-requests/{reqId}")
    public ApiResponse<?> getTransferRequest(@PathVariable String reqId) {
        return ApiResponse.success(assetService.getTransferRequest(reqId));
    }

    @Operation(summary = "Duyệt ticket (PENDING → APPROVED)")
    @PostMapping("/transfer-requests/{reqId}/approve")
    public ApiResponse<?> approveTransferRequest(@PathVariable String reqId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.approveTransferRequest(reqId, note));
    }

    @Operation(summary = "Từ chối ticket (PENDING → REJECTED)")
    @PostMapping("/transfer-requests/{reqId}/reject")
    public ApiResponse<?> rejectTransferRequest(@PathVariable String reqId,
                                                @RequestBody Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", null) : null;
        return ApiResponse.success(assetService.rejectTransferRequest(reqId, reason));
    }

    @Operation(summary = "Huỷ ticket (requester hoặc admin, chỉ khi PENDING)")
    @PostMapping("/transfer-requests/{reqId}/cancel")
    public ApiResponse<?> cancelTransferRequest(@PathVariable String reqId) {
        return ApiResponse.success(assetService.cancelTransferRequest(reqId));
    }

    @Operation(summary = "Xác nhận bàn giao (APPROVED → HANDED_OVER, update Asset)")
    @PostMapping("/transfer-requests/{reqId}/handover")
    public ApiResponse<?> handoverTransferRequest(@PathVariable String reqId,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.handoverTransferRequest(reqId, note));
    }
}
