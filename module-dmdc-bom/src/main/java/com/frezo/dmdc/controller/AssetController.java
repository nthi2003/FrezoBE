package com.frezo.dmdc.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
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
    @CheckPermission(api = "/qlts/asset", action = "VIEW")
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
    @CheckPermission(api = "/qlts/asset/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(assetService.getById(id));
    }

    @Operation(summary = "Timeline audit trail của 1 tài sản")
    @GetMapping("/{id}/history")
    @CheckPermission(api = "/qlts/asset/{id}/history", action = "VIEW")
    public ApiResponse<?> history(@PathVariable String id) {
        return ApiResponse.success(assetService.history(id));
    }

    @Operation(summary = "KPI stats cho card strip")
    @GetMapping("/stats")
    @CheckPermission(api = "/qlts/asset/stats", action = "VIEW")
    public ApiResponse<?> stats() {
        return ApiResponse.success(assetService.stats());
    }

    // ---- CRUD ----

    @Operation(summary = "Tạo tài sản mới")
    @PostMapping
    @CheckPermission(api = "/qlts/asset", action = "CREATE")
    public ApiResponse<?> create(@RequestBody AssetSaveRequest request) {
        return ApiResponse.success(assetService.create(request));
    }

    @Operation(summary = "Cập nhật thông tin tài sản (không đổi code, status)")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qlts/asset/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody AssetSaveRequest request) {
        return ApiResponse.success(assetService.update(id, request));
    }

    @Operation(summary = "Xoá mềm tài sản (chặn nếu đang cấp phát)")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qlts/asset/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        assetService.delete(id);
        return ApiResponse.success(null);
    }

    // ---- Workflow transitions ----

    @Operation(summary = "Cấp phát tài sản cho nhân viên")
    @PostMapping("/{id}/assign")
    @CheckPermission(api = "/qlts/asset/{id}/assign", action = "UPDATE")
    public ApiResponse<?> assign(@PathVariable String id, @RequestBody AssetAssignRequest request) {
        return ApiResponse.success(assetService.assign(id, request));
    }

    @Operation(summary = "Thu hồi tài sản — trở về AVAILABLE")
    @PostMapping("/{id}/unassign")
    @CheckPermission(api = "/qlts/asset/{id}/unassign", action = "UPDATE")
    public ApiResponse<?> unassign(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.unassign(id, note));
    }

    @Operation(summary = "Đưa vào bảo trì")
    @PostMapping("/{id}/maintenance/start")
    @CheckPermission(api = "/qlts/asset/{id}/maintenance/start", action = "UPDATE")
    public ApiResponse<?> startMaintenance(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.startMaintenance(id, note));
    }

    @Operation(summary = "Kết thúc bảo trì (option chi phí)")
    @PostMapping("/{id}/maintenance/end")
    @CheckPermission(api = "/qlts/asset/{id}/maintenance/end", action = "UPDATE")
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
    @CheckPermission(api = "/qlts/asset/{id}/dispose", action = "UPDATE")
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
    @CheckPermission(api = "/qlts/asset/{id}/transfer-requests", action = "CREATE")
    public ApiResponse<?> createTransferRequest(@PathVariable String id,
                                                @RequestBody AssetTransferCreateRequest request) {
        return ApiResponse.success(assetService.createTransferRequest(id, request));
    }

    @Operation(summary = "Danh sách ticket workflow")
    @GetMapping("/transfer-requests")
    @CheckPermission(api = "/qlts/asset/transfer-requests", action = "VIEW")
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
    @CheckPermission(api = "/qlts/asset/transfer-requests/{reqId}", action = "VIEW")
    public ApiResponse<?> getTransferRequest(@PathVariable String reqId) {
        return ApiResponse.success(assetService.getTransferRequest(reqId));
    }

    @Operation(summary = "Duyệt ticket (PENDING → APPROVED)")
    @PostMapping("/transfer-requests/{reqId}/approve")
    @CheckPermission(api = "/qlts/asset/transfer-requests/{reqId}/approve", action = "UPDATE")
    public ApiResponse<?> approveTransferRequest(@PathVariable String reqId,
                                                 @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.approveTransferRequest(reqId, note));
    }

    @Operation(summary = "Từ chối ticket (PENDING → REJECTED)")
    @PostMapping("/transfer-requests/{reqId}/reject")
    @CheckPermission(api = "/qlts/asset/transfer-requests/{reqId}/reject", action = "UPDATE")
    public ApiResponse<?> rejectTransferRequest(@PathVariable String reqId,
                                                @RequestBody Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", null) : null;
        return ApiResponse.success(assetService.rejectTransferRequest(reqId, reason));
    }

    @Operation(summary = "Huỷ ticket (requester hoặc admin, chỉ khi PENDING)")
    @PostMapping("/transfer-requests/{reqId}/cancel")
    @CheckPermission(api = "/qlts/asset/transfer-requests/{reqId}/cancel", action = "UPDATE")
    public ApiResponse<?> cancelTransferRequest(@PathVariable String reqId) {
        return ApiResponse.success(assetService.cancelTransferRequest(reqId));
    }

    @Operation(summary = "Xác nhận bàn giao (APPROVED → HANDED_OVER, update Asset)")
    @PostMapping("/transfer-requests/{reqId}/handover")
    @CheckPermission(api = "/qlts/asset/transfer-requests/{reqId}/handover", action = "UPDATE")
    public ApiResponse<?> handoverTransferRequest(@PathVariable String reqId,
                                                  @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.getOrDefault("note", null) : null;
        return ApiResponse.success(assetService.handoverTransferRequest(reqId, note));
    }
}
