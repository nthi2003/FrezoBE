package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TokenWalletResponse {
    private String id;
    private String personId;
    private String personName;
    private BigDecimal balance;
    private BigDecimal estimatedVnd;
    private BigDecimal tokenToVnd;
}
