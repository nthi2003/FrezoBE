package com.frezo.qtbv.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReactionType {
    HEART("HEART", "Yêu thích"),
    LIKE("LIKE", "Thích"),
    DISLIKE("DISLIKE", "Không thích");

    private final String code;
    private final String description;
}
