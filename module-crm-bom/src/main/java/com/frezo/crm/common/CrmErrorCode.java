package com.frezo.crm.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CrmErrorCode implements ErrorCode {

    LEAD_NOT_FOUND("crm.lead.not_found", HttpStatus.NOT_FOUND, "Lead không tồn tại"),
    LEAD_ALREADY_CONVERTED("crm.lead.already_converted", HttpStatus.CONFLICT, "Lead đã được convert"),

    PIPELINE_NOT_FOUND("crm.pipeline.not_found", HttpStatus.NOT_FOUND, "Pipeline không tồn tại"),
    STAGE_NOT_FOUND("crm.stage.not_found", HttpStatus.NOT_FOUND, "Stage không tồn tại"),

    DEAL_NOT_FOUND("crm.deal.not_found", HttpStatus.NOT_FOUND, "Deal không tồn tại"),
    DEAL_ALREADY_CLOSED("crm.deal.already_closed", HttpStatus.CONFLICT, "Deal đã đóng"),

    QUOTE_NOT_FOUND("crm.quote.not_found", HttpStatus.NOT_FOUND, "Báo giá không tồn tại"),
    INVOICE_NOT_FOUND("crm.invoice.not_found", HttpStatus.NOT_FOUND, "Hóa đơn không tồn tại"),
    INVOICE_ALREADY_POSTED("crm.invoice.already_posted", HttpStatus.CONFLICT, "Hóa đơn đã hạch toán");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
