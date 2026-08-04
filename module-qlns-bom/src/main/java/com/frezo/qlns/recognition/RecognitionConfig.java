package com.frezo.qlns.recognition;

import java.math.BigDecimal;

/**
 * Knobs cho module Ghi nhận / Token — có thể chuyển sang Setting.details sau.
 */
public final class RecognitionConfig {

    private RecognitionConfig() {}

    /** 1 token = bao nhiêu VND khi đổi thưởng. */
    public static final BigDecimal TOKEN_TO_VND = BigDecimal.valueOf(1_000L);

    /** Số token tối đa mỗi lần tặng. */
    public static final int MAX_GIFT_AMOUNT = 100;

    /** Số token tối đa mỗi lần đổi thưởng. */
    public static final int MAX_REDEEM_AMOUNT = 10_000;

    /** Số dư khởi tạo khi tạo ví lần đầu. */
    public static final BigDecimal STARTER_BALANCE = BigDecimal.valueOf(50);

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_TASK = "TASK";

    public static final String REDEEM_PENDING = "PENDING";
    public static final String REDEEM_APPROVED = "APPROVED";
    public static final String REDEEM_REJECTED = "REJECTED";
    public static final String REDEEM_PAID = "PAID";
}
