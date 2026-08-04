package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class RecognitionConfigResponse {
    private BigDecimal tokenToVnd;
    private Integer maxGiftAmount;
    private Integer maxRedeemAmount;
    private BigDecimal starterBalance;
}
