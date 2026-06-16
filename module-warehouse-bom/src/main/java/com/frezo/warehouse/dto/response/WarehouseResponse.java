package com.frezo.warehouse.dto.response;

import lombok.Data;

@Data
public class WarehouseResponse {
    private String id;
    private String code;
    private String name;
    private String shortName;
    private String type;
    private String addressLine;
    private String ward;
    private String district;
    private String province;
    private String countryCode;
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
