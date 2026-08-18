package com.frezo.profit.dto.request;

import com.frezo.profit.common.CostCategoryCode;
import com.frezo.profit.common.CostType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfitCostCategoryRequest {

    @NotNull
    private CostCategoryCode code;

    @NotBlank
    private String name;

    private String nameEn;

    @NotNull
    private CostType costType;

    private String currency;

    private String marketCode;

    private Integer sortOrder;

    private Boolean active;

    private String note;
}
