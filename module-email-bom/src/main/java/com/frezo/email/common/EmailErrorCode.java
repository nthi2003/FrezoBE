package com.frezo.email.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EmailErrorCode implements ErrorCode {

    TEMPLATE_NOT_FOUND("invalid.email.entity.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy mẫu email"),
    TEMPLATE_CODE_EXISTS("validate.code.exist", HttpStatus.CONFLICT, "Mã mẫu email đã tồn tại"),
    TEMPLATE_NAME_EXISTS("validate.name.exist", HttpStatus.CONFLICT, "Tên mẫu email đã tồn tại"),
    EMAIL_TEMPLATE_NOT_FOUND("error.email.template.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy template email"),

    CODE_EXISTS("valid.code.exists", HttpStatus.CONFLICT, "Mã cấu hình đã tồn tại"),
    NAME_EXISTS("valid.name.exists", HttpStatus.CONFLICT, "Tên cấu hình đã tồn tại"),
    SMTP_EXISTS("valid.smtp.exists", HttpStatus.CONFLICT, "SMTP đã tồn tại"),
    NAME_EMAIL_EXISTS("valid.nameEmail.exists", HttpStatus.CONFLICT, "Email gửi đã tồn tại"),
    CONFIG_ENTITY_NOT_FOUND("valid.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình email"),
    CONFIG_NOT_FOUND("error.email.config.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình email"),

    SEND_FAILED("error.email.send.failed", HttpStatus.INTERNAL_SERVER_ERROR, "Gửi email thất bại"),
    NO_RECIPIENTS("error.email.no.recipients", HttpStatus.BAD_REQUEST, "Không có người nhận"),
    CONNECTION_FAILED("error.email.connection.failed", HttpStatus.BAD_REQUEST, "Kết nối email thất bại"),
    EMAIL_NOT_FOUND("error.email.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy email"),
    INBOX_FETCH_FAILED("error.email.inbox.fetch.failed", HttpStatus.INTERNAL_SERVER_ERROR,
            "Lấy hộp thư thất bại: {0}"),
    INBOX_MARK_READ_FAILED("error.email.inbox.mark.read.failed", HttpStatus.INTERNAL_SERVER_ERROR,
            "Đánh dấu đã đọc thất bại: {0}");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
