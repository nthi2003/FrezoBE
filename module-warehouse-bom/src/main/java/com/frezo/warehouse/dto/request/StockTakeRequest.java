package com.frezo.warehouse.dto.request;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class StockTakeRequest {
    private String warehouseId;
    private LocalDate takeDate;
    private String note;
    private List<StockTakeLineRequest> lines;
}
