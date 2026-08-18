package com.frezo.profit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitFixedOverheadDto {
    private String id;
    private String categoryId;
    private String categoryName;
    private Integer year;
    private Integer month;
    private Double amount;
    private String currency;
    private String note;
}
