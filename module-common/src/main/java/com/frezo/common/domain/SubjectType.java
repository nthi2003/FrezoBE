package com.frezo.common.domain;

/**
 * Loại đối tượng gắn Approval / Comment — share giữa module-approval và CommentThread.
 * Giá trị string khớp FE {@code SubjectType} enum.
 */
public enum SubjectType {
    LEAVE,
    PAYROLL,
    DEAL,
    INVOICE,
    TICKET,
    CONTRACT,
    QUOTE,
    PURCHASE_REQUEST,
    RECRUITMENT,
    GENERIC
}
