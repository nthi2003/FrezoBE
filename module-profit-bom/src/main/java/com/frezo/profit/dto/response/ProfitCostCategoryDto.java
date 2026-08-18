package com.frezo.profit.dto.response;

import com.frezo.profit.common.CostCategoryCode;
import com.frezo.profit.common.CostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitCostCategoryDto {
    private String id;
    private CostCategoryCode code;
    private String name;
    private String nameEn;
    private CostType costType;
    private String currency;
    private String marketCode;
    private Integer sortOrder;
    private Boolean active;
    private String note;
}
