package com.frezo.qtht.common;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DepartmentStatus {
    ACTIVE("ACTIVE", "Hoạt động"),
    INACTIVE("INACTIVE", "Không hoạt động"),
    SUSPENDED("SUSPENDED", "Tạm ngừng"),
    MERGED("MERGED", "Đã sáp nhập"),
    DISSOLVED("DISSOLVED", "Đã giải thể");

    private final String code;
    private final String description;
}
