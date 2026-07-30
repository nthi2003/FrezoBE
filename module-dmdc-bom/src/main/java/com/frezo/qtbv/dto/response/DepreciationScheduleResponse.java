package com.frezo.qtbv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepreciationScheduleResponse {

    private String id;
    private String assetId;
    private String assetCode;
    private String assetName;
    private String method;
    private LocalDate startDate;
    private Integer months;
    /** Nguyên giá (purchasePrice của tài sản) — phục vụ bảng dòng khấu hao. */
    private BigDecimal purchasePrice;
    private BigDecimal monthlyAmount;
    private BigDecimal remainingValue;
    private String status;
}
