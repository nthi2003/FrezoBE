package com.frezo.qtht.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrganizationStatus {
    ACTIVE("ACTIVE", "Hoạt động"),
    INACTIVE("INACTIVE", "Không hoạt động"),
    SUSPENDED("SUSPENDED", "Tạm ngừng"),
    MERGED("MERGED", "Đã sáp nhập"),
    ACQUIRED("ACQUIRED", "Đã được mua lại"),
    DISSOLVED("DISSOLVED", "Đã giải thể"),
    LIQUIDATED("LIQUIDATED", "Đã thanh lý");

    private final String code;
    private final String description;
}
