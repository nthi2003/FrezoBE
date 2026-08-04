package com.frezo.task.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TaskErrorCode implements ErrorCode {

    // -------------------- TASK --------------------
    TASK_NOT_FOUND("TASK_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy công việc với ID: {0}"),
    INVALID_STATUS("INVALID_STATUS", HttpStatus.BAD_REQUEST, "Trạng thái không hợp lệ: {0}"),

    // -------------------- TAG --------------------
    TAG_CODE_EXISTS("TAG_CODE_EXISTS", HttpStatus.CONFLICT, "Mã nhãn đã tồn tại: {0}"),
    TAG_NOT_FOUND("TAG_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy nhãn với ID: {0}"),

    // -------------------- TICKET --------------------
    TICKET_NOT_FOUND("Ticket not found", HttpStatus.NOT_FOUND, "Không tìm thấy ticket"),
    TICKET_STATUS_INVALID("Invalid ticket status", HttpStatus.BAD_REQUEST, "Trạng thái ticket không hợp lệ"),
    TICKET_CATEGORY_INVALID("TICKET_CATEGORY_INVALID", HttpStatus.BAD_REQUEST, "Danh mục không hợp lệ: {0}"),
    TICKET_ACCESS_DENIED("TICKET_ACCESS_DENIED", HttpStatus.FORBIDDEN, "Bạn không có quyền xem ticket này"),
    TICKET_COMPLETE_FORBIDDEN("TICKET_COMPLETE_FORBIDDEN", HttpStatus.FORBIDDEN, "Chỉ người được giao hoặc admin mới đánh dấu hoàn thành"),
    TICKET_REVIEW_FORBIDDEN("TICKET_REVIEW_FORBIDDEN", HttpStatus.FORBIDDEN, "Chỉ người giao việc hoặc admin mới duyệt hoàn thành"),
    TICKET_REVIEW_INVALID("TICKET_REVIEW_INVALID", HttpStatus.BAD_REQUEST, "Ticket phải ở trạng thái Đã giải quyết mới được duyệt"),
    TASK_ACCESS_DENIED("TASK_ACCESS_DENIED", HttpStatus.FORBIDDEN, "Bạn không có quyền xem công việc này"),
    TASK_COMPLETE_FORBIDDEN("TASK_COMPLETE_FORBIDDEN", HttpStatus.FORBIDDEN, "Chỉ người được giao hoặc admin mới đánh dấu hoàn thành"),
    TASK_REVIEW_FORBIDDEN("TASK_REVIEW_FORBIDDEN", HttpStatus.FORBIDDEN, "Chỉ người giao việc hoặc admin mới duyệt hoàn thành"),
    TASK_REVIEW_INVALID("TASK_REVIEW_INVALID", HttpStatus.BAD_REQUEST, "Công việc phải ở trạng thái Hoàn thành mới được duyệt"),

    // -------------------- TICKET CATEGORY --------------------
    TICKET_CATEGORY_CODE_EXISTS("TICKET_CATEGORY_CODE_EXISTS", HttpStatus.CONFLICT, "Mã danh mục đã tồn tại: {0}"),
    TICKET_CATEGORY_NOT_FOUND("TICKET_CATEGORY_NOT_FOUND", HttpStatus.NOT_FOUND, "Không tìm thấy danh mục: {0}"),
    TICKET_CATEGORY_CODE_REQUIRED("TICKET_CATEGORY_CODE_REQUIRED", HttpStatus.BAD_REQUEST, "Mã danh mục bắt buộc"),
    TICKET_CATEGORY_NAME_REQUIRED("TICKET_CATEGORY_NAME_REQUIRED", HttpStatus.BAD_REQUEST, "Tên danh mục bắt buộc");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
