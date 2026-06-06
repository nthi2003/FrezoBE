package com.frezo.qtht.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrganizationScale {
    MICRO("MICRO", "Siêu nhỏ (dưới 10 người)"),
    SMALL("SMALL", "Nhỏ (10-49 người)"),
    MEDIUM("MEDIUM", "Vừa (50-249 người)"),
    LARGE("LARGE", "Lớn (250-999 người)"),
    ENTERPRISE("ENTERPRISE", "Doanh nghiệp lớn (1000+ người)"),
    CORPORATION("CORPORATION", "Tập đoàn");

    private final String code;
    private final String description;
}
