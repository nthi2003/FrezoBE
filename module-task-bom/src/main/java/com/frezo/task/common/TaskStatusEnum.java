package com.frezo.task.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatusEnum {
    OPEN("OPEN", "Mở"),
    IN_PROGRESS("IN_PROGRESS", "Đang thực hiện"),
    /** EU đánh dấu xong — chờ người giao / admin duyệt. */
    DONE("DONE", "Hoàn thành (chờ duyệt)"),
    /** Người giao đã xác nhận hoàn thành. */
    CLOSED("CLOSED", "Đã đóng"),
    CANCELLED("CANCELLED", "Hủy");

    private final String code;
    private final String description;
}
