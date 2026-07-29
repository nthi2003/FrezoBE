package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTakeResponse {
    private String id;
    private String code;
    private String warehouseId;
    private String warehouseName;
    private String warehouseCode;
    private LocalDate takeDate;
    private String status;
    private String note;
    private List<StockTakeLineResponse> lines;
}
