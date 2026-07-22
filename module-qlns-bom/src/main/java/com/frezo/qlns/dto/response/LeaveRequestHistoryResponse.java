package com.frezo.qlns.dto.response;

import lombok.Data;

/**
 * Item trên timeline duyệt đơn nghỉ phép.
 * FE render dạng Slack thread — mỗi item = 1 dot + line + text.
 */
@Data
public class LeaveRequestHistoryResponse {
    private String id;
    /** SUBMIT | APPROVE | REJECT | CANCEL | AUTO_ROUTE | REASSIGN */
    private String action;
    private String fromStatus;
    private String toStatus;
    private String actorUsername;
    /** REQUESTER | MANAGER | HR | SYSTEM — FE map ra badge màu. */
    private String actorRole;
    private String comment;
    /** ISO-8601 timestamp — FE tự format. */
    private String createdDate;
}
