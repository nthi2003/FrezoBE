package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CategoryUsageResponse {
    private String categoryCode;
    private long usageCount;
    private List<String> positionNames;
    private String message;
}
