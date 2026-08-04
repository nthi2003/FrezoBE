package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TokenRewardCatalogResponse {
    private String id;
    private String code;
    private String name;
    private BigDecimal tokenCost;
    private BigDecimal cashValue;
    private String description;
}
