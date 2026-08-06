package com.frezo.common.constant;

import lombok.Getter;

@Getter
public enum BlockReason {
    BRUTE_FORCE,          // Sai password nhiều lần
    WRONG_PASSWORD,
    OTP_BRUTE_FORCE,      // Sai OTP quên mật khẩu nhiều lần

}
