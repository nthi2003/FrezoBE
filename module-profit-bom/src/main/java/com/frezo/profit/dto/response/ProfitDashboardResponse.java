package com.frezo.profit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitDashboardResponse {

    private Integer year;
    private Integer month;
    private String currency;

    /** Doanh thu thực tế từ đơn bán hàng */
    private Double actualRevenue;

    /** Doanh thu cần để hòa vốn (cover monthly burn) */
    private Double breakEvenRevenue;

    /** Doanh thu mục tiêu (break-even + biên LN mục tiêu) */
    private Double targetRevenue;

    /** Còn thiếu bao nhiêu để hòa vốn */
    private Double revenueShortfall;

    /** Còn thiếu bao nhiêu để đạt mục tiêu LN */
    private Double targetShortfall;

    /** Chi phí cố định tháng (overhead + lương fixed roles) */
    private Double totalFixedCosts;

    /** Chi phí biến đổi tính trên doanh thu thực tế */
    private Double totalVariableCosts;

    /** Giá vốn ước tính */
    private Double estimatedCogs;

    /** Lợi nhuận ròng ước tính */
    private Double netProfit;

    /** Thuế TNDN ước tính */
    private Double corporateTaxAmount;

    /** Lợi nhuận sau thuế */
    private Double netProfitAfterTax;

    private Double corporateTaxPct;

    /** Monthly burn = fixed + sales salaries (base obligations) */
    private Double monthlyBurn;

    /** Biên đóng góp = 1 − COGS% − risk% − commission% */
    private Double contributionMarginPct;

    private Integer employeeCount;
    private Integer salesStaffCount;

    private List<CostBreakdownLine> costBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostBreakdownLine {
        private String code;
        private String label;
        private String costType;
        private Double amount;
    }
}
