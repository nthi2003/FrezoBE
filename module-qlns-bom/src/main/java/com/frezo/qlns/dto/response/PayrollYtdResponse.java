package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Year-to-date summary — tổng lũy kế + list 12 tháng để vẽ chart trên Mobile.
 */
@Data
@Builder
public class PayrollYtdResponse {

    private String personId;
    private Integer year;

    /** Tổng thu nhập lũy kế (gross) trong năm. */
    private BigDecimal ytdGross;

    /** Tổng khấu trừ (BH + PIT). */
    private BigDecimal ytdInsurance;
    private BigDecimal ytdPit;

    /** Tổng thực nhận. */
    private BigDecimal ytdNet;

    /** Số tháng đã có bảng lương. */
    private Integer monthsProcessed;

    /** Chi tiết từng tháng (fill 0 cho tháng chưa có payroll). */
    private List<Month> months;

    @Data
    @Builder
    public static class Month {
        private Integer month;
        private BigDecimal gross;
        private BigDecimal net;
        private BigDecimal insurance;
        private BigDecimal pit;
        private Boolean processed;
    }
}
