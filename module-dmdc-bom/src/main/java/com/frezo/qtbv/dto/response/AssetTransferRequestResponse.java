package com.frezo.qtbv.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetTransferRequestResponse {
    private String id;
    private String assetId;
    /** Enrich: mã tài sản để hiển thị nhanh trên UI. */
    private String assetCode;
    /** Enrich: tên tài sản. */
    private String assetName;

    private String requestType;
    private String status;

    private String requesterUsername;

    private String personId;
    private String personName;

    private String reason;
    private LocalDate plannedDate;

    private String approvedBy;
    private String approvedAt;
    private String approveNote;

    private String rejectedBy;
    private String rejectedAt;
    private String rejectReason;

    private String cancelledAt;

    private String handedOverBy;
    private String handedOverAt;
    private String handoverNote;

    private String createdBy;
    private String createdDate;

    // ---- Workflow engine (v2) ----
    /**
     * ID instance của workflow engine — nếu != null, FE nên gọi
     * {@code GET /wf/instances/by-entity/ASSET_TRANSFER/{id}} để lấy state chi tiết
     * (steps, current task, approver types...) và render {@code <WorkflowStepper />}
     * động thay vì hard-code 3 bước.
     */
    private String workflowInstanceId;

    /** entityType để FE query engine — luôn = "ASSET_TRANSFER". */
    private String workflowEntityType;

    /** Task PENDING hiện tại (nếu có) — FE dùng để hiển thị nút Duyệt/Từ chối. */
    private String currentTaskId;

    /** Tên bước hiện tại — VD "Duyệt yêu cầu", "Bàn giao" — hiển thị trên nút. */
    private String currentStepName;
}
