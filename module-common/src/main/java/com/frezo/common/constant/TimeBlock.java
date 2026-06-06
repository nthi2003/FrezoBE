package com.frezo.common.constant;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TimeBlock {

    LEVEL_1(1, 5), // Lần 1: khóa 5 phút
    LEVEL_2(2, 15), // Lần 2: khóa 15 phút
    LEVEL_3(3, 30), // Lần 3: khóa 30 phút
    LEVEL_4(4, 60), // Lần 4: khóa 1 giờ
    LEVEL_MAX(5, 1440); // Lần 5+: khóa 24 giờ

    private final int level;
    private final int durationMinutes;
}
