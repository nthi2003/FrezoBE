package com.frezo.fbautomation.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FbAutomationErrorCode implements ErrorCode {

    LEAD_ALREADY_IMPORTED("fb.lead.already.imported", HttpStatus.CONFLICT, "Lead này đã được import trước đó"),
    LEAD_NOT_FOUND("fb.lead.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy lead Facebook"),
    GROUP_NOT_FOUND("fb.group.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy group Facebook"),
    GROUP_ID_NOT_FOUND("fb.group.id.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy group: {0}"),
    ACCOUNT_NOT_FOUND("fb.account.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản Facebook"),
    ACCOUNT_EXISTS("fb.account.exists", HttpStatus.CONFLICT, "Tài khoản {0} đã tồn tại"),
    SCRAPE_GROUPS_FAILED("fb.scrape.groups.failed", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi scrape groups: {0}"),
    JOIN_GROUP_FAILED("fb.join.group.failed", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi tham gia group: {0}");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
