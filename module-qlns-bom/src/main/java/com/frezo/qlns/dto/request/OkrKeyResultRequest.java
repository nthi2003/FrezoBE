package com.frezo.qlns.dto.request;

import lombok.Data;

@Data
public class OkrKeyResultRequest {
    private String id;
    private String title;
    private Double targetValue;
    private Double currentValue;
    private String unit;
    private Integer sortOrder;
}
