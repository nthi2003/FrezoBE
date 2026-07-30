package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResignationResponse {

    private String id;
    private String requestCode;
    private String personId;
    private String personName;
    private String expectedLastDay;
    private String actualLastDay;
    private String reason;
    private String status;
    private String managerApprovedBy;
    private String managerApprovedAt;
    private String hrConfirmedBy;
    private String hrConfirmedAt;
    private Boolean laptopReturned;
    private Boolean badgeReturned;
    private Boolean docsHandedOver;
    private String handoverNote;
    private String handoverAt;
    private String payrollSettledAt;
    private String userRevokedAt;
    private String completedAt;
    private String createdDate;
    private String createdBy;
}
