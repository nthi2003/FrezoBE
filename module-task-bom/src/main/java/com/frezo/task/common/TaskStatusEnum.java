package com.frezo.task.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStatusEnum {
    OPEN("OPEN", "Mở"),
    IN_PROGRESS("IN_PROGRESS", "Đang thực hiện"),
    DONE("DONE", "Hoàn thành"),
    CANCELLED("CANCELLED", "Hủy");

    private final String code;
    private final String description;
}
