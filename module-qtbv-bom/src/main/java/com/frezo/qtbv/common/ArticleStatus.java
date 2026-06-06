package com.frezo.qtbv.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ArticleStatus {
    DRAFT("DRAFT", "Bản nháp"),
    WAITING_APPROVAL("WAITING_APPROVAL", "Chờ phê duyệt"),
    APPROVED("APPROVED", "Đã phê duyệt"),
    PUBLISHED("PUBLISHED", "Đã xuất bản"),
    REJECTED("REJECTED", "Bị từ chối"),
    DELETED("DELETED", "Đã xóa");

    private final String code;
    private final String description;
}
