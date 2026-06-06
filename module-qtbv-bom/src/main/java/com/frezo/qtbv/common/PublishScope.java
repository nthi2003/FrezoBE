package com.frezo.qtbv.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PublishScope {
    INTERNAL("INTERNAL", "Nội bộ"),
    PUBLIC("PUBLIC", "Công khai");

    private final String code;
    private final String description;
}
