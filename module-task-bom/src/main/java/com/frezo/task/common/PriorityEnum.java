package com.frezo.task.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PriorityEnum {
    HIGH ("HIGH", "Cao"),
    MEDIUM("MEDIUM", "Trung bình"),
    LOW("LOW", "Thấp");

    private final String code;
    private final String description;

}
