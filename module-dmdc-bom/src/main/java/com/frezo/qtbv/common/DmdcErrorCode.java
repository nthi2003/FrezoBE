package com.frezo.qtbv.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DmdcErrorCode implements ErrorCode {

    // -------------------- CATEGORY --------------------
    CATEGORY_CODE_EXISTS("category.code.exist", HttpStatus.CONFLICT, "Mã danh mục đã tồn tại"),
    CATEGORY_NAME_EXISTS("category.name.exist", HttpStatus.CONFLICT, "Tên danh mục đã tồn tại"),
    CATEGORY_NAME_EN_EXISTS("category.name.en.exist", HttpStatus.CONFLICT, "Tên EN danh mục đã tồn tại"),
    ENTITY_NOT_FOUND("valid.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),

    // -------------------- ASSET --------------------
    ASSET_NOT_FOUND("error.asset.not.found", HttpStatus.NOT_FOUND, "Không tìm thấy tài sản"),
    ASSET_CODE_DUPLICATE("error.asset.code.duplicate", HttpStatus.CONFLICT, "Mã tài sản đã tồn tại"),
    ASSET_IN_USE_CANNOT_DELETE("error.asset.in.use.cannot.delete", HttpStatus.CONFLICT,
            "Không thể xóa tài sản đang sử dụng"),
    ASSET_NOT_AVAILABLE("error.asset.not.available", HttpStatus.BAD_REQUEST, "Tài sản không sẵn sàng"),
    ASSET_ASSIGN_PERSON_REQUIRED("error.asset.assign.person.required", HttpStatus.BAD_REQUEST,
            "Bắt buộc chọn nhân sự khi gán tài sản"),
    ASSET_NOT_IN_USE("error.asset.not.in.use", HttpStatus.BAD_REQUEST, "Tài sản không đang sử dụng"),
    ASSET_DISPOSED("error.asset.disposed", HttpStatus.BAD_REQUEST, "Tài sản đã thanh lý"),
    ASSET_NOT_IN_MAINTENANCE("error.asset.not.in.maintenance", HttpStatus.BAD_REQUEST,
            "Tài sản không đang bảo trì"),
    ASSET_ALREADY_DISPOSED("error.asset.already.disposed", HttpStatus.CONFLICT, "Tài sản đã được thanh lý"),

    // -------------------- ASSET TRANSFER --------------------
    TRANSFER_ACTIVE_EXISTS("error.asset.transfer.active.exists", HttpStatus.CONFLICT,
            "Đã có yêu cầu điều chuyển đang xử lý"),
    TRANSFER_TYPE_INVALID("error.asset.transfer.type.invalid", HttpStatus.BAD_REQUEST,
            "Loại điều chuyển không hợp lệ"),
    TRANSFER_NOT_PENDING("error.asset.transfer.not.pending", HttpStatus.BAD_REQUEST,
            "Yêu cầu điều chuyển không ở trạng thái chờ duyệt"),
    TRANSFER_REJECT_REASON_REQUIRED("error.asset.transfer.reject.reason.required", HttpStatus.BAD_REQUEST,
            "Bắt buộc nhập lý do từ chối"),
    TRANSFER_NO_PENDING_TASK("error.asset.transfer.no.pending.task", HttpStatus.BAD_REQUEST,
            "Không có task duyệt đang chờ"),
    TRANSFER_CANCEL_FORBIDDEN("error.asset.transfer.cancel.forbidden", HttpStatus.FORBIDDEN,
            "Không có quyền hủy yêu cầu điều chuyển"),
    TRANSFER_CANNOT_CANCEL("error.asset.transfer.cannot.cancel", HttpStatus.BAD_REQUEST,
            "Không thể hủy yêu cầu điều chuyển"),
    TRANSFER_NOT_APPROVED("error.asset.transfer.not.approved", HttpStatus.BAD_REQUEST,
            "Yêu cầu điều chuyển chưa được duyệt"),
    TRANSFER_NOT_FOUND("error.asset.transfer.not.found", HttpStatus.NOT_FOUND,
            "Không tìm thấy yêu cầu điều chuyển"),
    TRANSFER_APPROVE_FORBIDDEN("error.asset.transfer.approve.forbidden", HttpStatus.FORBIDDEN,
            "Không có quyền duyệt yêu cầu điều chuyển");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
