package com.frezo.qtbv.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DepreciationScheduleRequest {

    private String assetId;

    private String method;

    private LocalDate startDate;

    private Integer months;
}
