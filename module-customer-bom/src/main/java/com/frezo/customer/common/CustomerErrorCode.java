package com.frezo.customer.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CustomerErrorCode implements ErrorCode {

    // -------------------- CUSTOMER --------------------
    CUSTOMER_NOT_FOUND("exception.customer.not_found", HttpStatus.NOT_FOUND, "Không tìm thấy khách hàng"),
    CUSTOMER_CODE_EXISTS("exception.customer.code.exists", HttpStatus.CONFLICT, "Mã khách hàng đã tồn tại"),
    CUSTOMER_IMPORT_FAILED("exception.customer.import.failed", HttpStatus.INTERNAL_SERVER_ERROR,
            "Import khách hàng thất bại"),
    CUSTOMER_EXPORT_FAILED("exception.customer.export.failed", HttpStatus.INTERNAL_SERVER_ERROR,
            "Export khách hàng thất bại"),
    AI_CONNECTION_FAILED("customer.ai.connection.failed", HttpStatus.BAD_GATEWAY,
            "Kết nối tới hệ thống AI thất bại"),

    // -------------------- VOUCHER --------------------
    VOUCHER_NOT_FOUND("exception.voucher.not_found", HttpStatus.NOT_FOUND, "Không tìm thấy voucher"),
    VOUCHER_CODE_EXISTS("exception.voucher.code.exists", HttpStatus.CONFLICT, "Mã voucher đã tồn tại"),
    VOUCHER_INACTIVE("exception.voucher.inactive", HttpStatus.BAD_REQUEST, "Voucher không còn hiệu lực"),
    VOUCHER_EXPIRED("exception.voucher.expired", HttpStatus.BAD_REQUEST, "Voucher đã hết hạn"),
    VOUCHER_NOT_STARTED("exception.voucher.not_started", HttpStatus.BAD_REQUEST, "Voucher chưa bắt đầu"),
    VOUCHER_MIN_ORDER_NOT_MET("exception.voucher.min_order_not_met", HttpStatus.BAD_REQUEST,
            "Chưa đạt giá trị đơn tối thiểu"),
    VOUCHER_MAX_USAGE("exception.voucher.max_usage", HttpStatus.BAD_REQUEST, "Voucher đã hết lượt sử dụng"),

    // -------------------- NCC --------------------
    NCC_NOT_FOUND("exception.ncc.not_found", HttpStatus.NOT_FOUND, "Không tìm thấy nhà cung cấp"),
    NCC_CODE_EXISTS("exception.ncc.code.exists", HttpStatus.CONFLICT, "Mã nhà cung cấp đã tồn tại");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
