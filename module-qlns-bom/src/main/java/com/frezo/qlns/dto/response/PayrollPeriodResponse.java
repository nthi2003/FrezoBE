package com.frezo.qlns.dto.response;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PayrollPeriodResponse {
    private String id;
    private String orgId;
    private Integer month;
    private Integer year;
    private String name;
    private Integer status;
    private String statusLabel;
    private LocalDate fromDate;
    private LocalDate toDate;
    private LocalDate paymentDate;
    private LocalDateTime lockedAt;
    private String lockedBy;
    private String note;

    // ---- Workflow engine enrichment ----
    /** ID instance workflow đang chạy — null nếu chưa duyệt / kỳ tạo trước khi wire. */
    private String workflowInstanceId;
    /** ID ApprovalRequest (module-approval). */
    private String approvalRequestId;
    /** entityType cho engine query — luôn "PAYROLL_PERIOD" khi có instance. */
    private String workflowEntityType;
    /** Task PENDING hiện tại — dùng để render nút "Duyệt: {stepName}". */
    private String currentTaskId;
    /** Tên bước đang chờ duyệt (VD "Kế toán duyệt", "Giám đốc ký"). */
    private String currentStepName;
}
