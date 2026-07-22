package com.frezo.qtbv.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetResponse {
    private String id;
    private String code;
    private String name;
    private String categoryCode;
    /** Tên category — service enrich để FE khỏi phải join. */
    private String categoryName;
    private String brand;
    private String model;
    private String serialNumber;
    private LocalDate purchaseDate;
    private BigDecimal purchasePrice;
    private BigDecimal currentValue;
    private LocalDate warrantyEndDate;
    private String status;
    private String location;
    private String assignedPersonId;
    /** Tên Person đang giữ — enrich. */
    private String assignedPersonName;
    private LocalDate assignedAt;
    private String imageUrl;
    private String note;
    private String createdBy;
    private String createdDate;
}
