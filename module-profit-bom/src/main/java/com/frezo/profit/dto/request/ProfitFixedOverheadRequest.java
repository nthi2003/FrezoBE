package com.frezo.profit.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfitFixedOverheadRequest {

    @NotBlank
    private String categoryId;

    @NotNull
    private Integer year;

    @NotNull
    private Integer month;

    @NotNull
    private Double amount;

    private String currency;

    private String note;
}
