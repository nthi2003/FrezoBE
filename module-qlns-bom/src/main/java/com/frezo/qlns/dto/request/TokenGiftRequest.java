package com.frezo.qlns.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TokenGiftRequest {
    private String toPersonId;
    private BigDecimal amount;
    private String note;
    /** MANUAL | TASK — default MANUAL */
    private String sourceType;
    private String sourceId;
}
