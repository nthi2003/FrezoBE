package com.frezo.warehouse.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WarehouseErrorCode implements ErrorCode {

    // -------------------- WAREHOUSE --------------------
    WAREHOUSE_NOT_FOUND("warehouse.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy kho"),
    WAREHOUSE_CODE_EXISTS("warehouse.code.exists", HttpStatus.BAD_REQUEST, "Mã kho đã tồn tại"),
    ZONE_NOT_FOUND("warehouse.zone.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy khu vực kho"),
    LOCATION_NOT_FOUND("warehouse.location.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy vị trí kho"),

    // -------------------- STOCK BALANCE --------------------
    STOCK_BALANCE_NOT_FOUND("stock.balance.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy tồn kho"),
    STOCK_BALANCE_INSUFFICIENT("stock.balance.insufficient", HttpStatus.BAD_REQUEST, "Tồn kho không đủ"),
    STOCK_EXPORT_FAILED("stock.export.failed", HttpStatus.INTERNAL_SERVER_ERROR, "Xuất báo cáo kho thất bại"),

    // -------------------- TRANSFER --------------------
    TRANSFER_NOT_FOUND("stock.transfer.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy phiếu chuyển kho"),
    TRANSFER_SAME_WAREHOUSE("stock.transfer.same.warehouse", HttpStatus.BAD_REQUEST, "Kho nguồn và kho đích trùng nhau"),
    TRANSFER_ALREADY_CONFIRMED("stock.transfer.already.confirmed", HttpStatus.BAD_REQUEST, "Phiếu chuyển kho đã xác nhận"),
    TRANSFER_CANNOT_CANCEL_CONFIRMED("stock.transfer.cannot.cancel.confirmed", HttpStatus.BAD_REQUEST,
            "Không thể hủy phiếu chuyển kho đã xác nhận"),
    TRANSFER_CANNOT_DELETE_CONFIRMED("stock.transfer.cannot.delete.confirmed", HttpStatus.BAD_REQUEST,
            "Không thể xóa phiếu chuyển kho đã xác nhận"),

    // -------------------- ADJUSTMENT --------------------
    ADJUSTMENT_NOT_FOUND("stock.adjustment.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy phiếu điều chỉnh"),
    ADJUSTMENT_ALREADY_CONFIRMED("stock.adjustment.already.confirmed", HttpStatus.BAD_REQUEST,
            "Phiếu điều chỉnh đã xác nhận"),
    ADJUSTMENT_CANNOT_CANCEL_CONFIRMED("stock.adjustment.cannot.cancel.confirmed", HttpStatus.BAD_REQUEST,
            "Không thể hủy phiếu điều chỉnh đã xác nhận"),
    ADJUSTMENT_CANNOT_DELETE_CONFIRMED("stock.adjustment.cannot.delete.confirmed", HttpStatus.BAD_REQUEST,
            "Không thể xóa phiếu điều chỉnh đã xác nhận"),

    // -------------------- GRN --------------------
    GRN_NOT_FOUND("goods.receipt.note.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy phiếu nhập kho"),
    GRN_ALREADY_CONFIRMED("goods.receipt.note.already.confirmed", HttpStatus.BAD_REQUEST,
            "Phiếu nhập kho đã xác nhận"),
    GRN_CANNOT_CANCEL_CONFIRMED("goods.receipt.note.cannot.cancel.confirmed", HttpStatus.BAD_REQUEST,
            "Không thể hủy phiếu nhập kho đã xác nhận"),
    GRN_CANNOT_DELETE_CONFIRMED("goods.receipt.note.cannot.delete.confirmed", HttpStatus.BAD_REQUEST,
            "Không thể xóa phiếu nhập kho đã xác nhận"),
    GRN_INVALID_STATUS("goods.receipt.note.invalid.status", HttpStatus.BAD_REQUEST,
            "Trạng thái phiếu nhập kho không hợp lệ cho thao tác này"),
    GRN_INVOICE_REQUIRED("goods.receipt.note.invoice.required", HttpStatus.BAD_REQUEST,
            "Phiếu nhập kho gắn NCC/PO phải có số hóa đơn NCC trước khi xác nhận nhập"),

    // -------------------- GIN --------------------
    GIN_NOT_FOUND("goods.issue.note.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy phiếu xuất kho"),
    GIN_ALREADY_CONFIRMED("goods.issue.note.already.confirmed", HttpStatus.BAD_REQUEST,
            "Phiếu xuất kho đã xác nhận"),
    GIN_CANNOT_CANCEL_CONFIRMED("goods.issue.note.cannot.cancel.confirmed", HttpStatus.BAD_REQUEST,
            "Không thể hủy phiếu xuất kho đã xác nhận"),
    GIN_CANNOT_DELETE_CONFIRMED("goods.issue.note.cannot.delete.confirmed", HttpStatus.BAD_REQUEST,
            "Không thể xóa phiếu xuất kho đã xác nhận"),
    GIN_INVALID_STATUS("goods.issue.note.invalid.status", HttpStatus.BAD_REQUEST,
            "Trạng thái phiếu xuất kho không hợp lệ cho thao tác này");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
