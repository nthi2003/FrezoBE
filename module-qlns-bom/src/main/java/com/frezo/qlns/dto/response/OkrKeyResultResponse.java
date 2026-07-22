package com.frezo.qlns.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OkrKeyResultResponse {
    private String id;
    private String title;
    private Double targetValue;
    private Double currentValue;
    private String unit;
    private Double progress;
    private Integer sortOrder;
}
