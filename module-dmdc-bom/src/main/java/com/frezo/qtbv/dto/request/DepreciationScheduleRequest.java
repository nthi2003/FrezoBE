package com.frezo.qtbv.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class DepreciationScheduleRequest {

    private String assetId;

    /** STRAIGHT_LINE (mặc định) / DECLINING. */
    private String method;

    /** Nếu null → dùng {@code purchaseDate} của asset (hoặc hôm nay). */
    private LocalDate startDate;

    /** Số tháng khấu hao. */
    private Integer months;
}
