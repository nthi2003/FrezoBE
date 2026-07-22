package com.frezo.qtbv.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetSaveRequest {
    /** Nullable — nếu blank, service auto-gen dạng AS-YYYY-####. */
    private String code;
    private String name;
    private String categoryCode;
    private String brand;
    private String model;
    private String serialNumber;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private BigDecimal currentValue;
    private LocalDate warrantyEndDate;
    /** Default AVAILABLE nếu null. */
    private String status;
    private String location;
    private String imageUrl;
    private String note;
}
