package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Mô tả công thức tính từng khoản trên payslip. Static — dùng cho popup "Giải thích công thức" trên mobile.
 */
@Data
@Builder
public class PayslipFormulaResponse {
    private List<Formula> formulas;

    @Data
    @Builder
    public static class Formula {
        /** Key trùng với field trên PayslipResponse (VD: "socialInsurance", "taxIncome"). */
        private String key;
        private String label;
        private String formula;
        private String explanation;
    }
}
