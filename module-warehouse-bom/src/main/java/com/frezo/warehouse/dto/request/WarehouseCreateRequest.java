package com.frezo.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseCreateRequest {
    @NotBlank(message = "Mã kho không được để trống")
    private String code;

    @NotBlank(message = "Tên kho không được để trống")
    private String name;

    private String shortName;
    private String type;
    private String addressLine;
    private String ward;
    private String district;
    private String province;
    private String countryCode;
    private Double latitude;
    private Double longitude;
    private String managerId;
    private String phone;
    private String email;
    private Double totalAreaSqm;
    private Double maxCapacity;
    private String capacityUnit;
    private Double temperatureMin;
    private Double temperatureMax;
    private Boolean isColdStorage;
    private String status;
    private Boolean isDefault;
    private String note;
}
