package com.frezo.qlns.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TokenRedeemCreateRequest {
    private BigDecimal amount;
    private String note;
    /** Optional catalog item — MVP ignore nếu null. */
    private String catalogId;
}
