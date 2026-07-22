package com.frezo.accounting.seed;

import com.frezo.accounting.common.AccountType;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Mô tả 1 tài khoản trong dữ liệu seed. Dùng để nạp COA theo chuẩn.
 */
@Getter
@AllArgsConstructor
public class CoaSeedItem {
    private final String code;
    private final String name;
    private final AccountType type;
    private final int level;
    private final String parentCode;
    private final boolean postable;
    private final boolean requiresPartner;
}
