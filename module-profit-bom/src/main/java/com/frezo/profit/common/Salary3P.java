package com.frezo.profit.common;

import com.frezo.qlns.entity.Contract;


public record Salary3P(double p1Position, double p2Person, double p3Performance) {

    public double fixedMonthly() {
        return p1Position + p2Person;
    }

    public double totalAtFullKpi() {
        return p1Position + p2Person + p3Performance;
    }

    public static Salary3P fromContract(Contract contract) {
        if (contract == null) {
            return new Salary3P(0, 0, 0);
        }
        int p1 = contract.getPositionSalary() != null
                ? contract.getPositionSalary()
                : (contract.getValue() != null ? contract.getValue() : 0);
        int p2 = contract.getPersonAllowance() != null ? contract.getPersonAllowance() : 0;
        int p3 = contract.getKpiBonus() != null ? contract.getKpiBonus() : 0;
        return new Salary3P(p1, p2, p3);
    }

    public static double revenueTargetForFullKpi(Salary3P salary, double cogsPct, double riskPct, double commissionPct) {
        double margin = contributionMarginPct(cogsPct, riskPct, commissionPct);
        if (margin <= 0) {
            return salary.totalAtFullKpi();
        }
        return salary.totalAtFullKpi() / (margin / 100d);
    }


    public static double p3Earned(Salary3P salary, double achievementPct) {
        if (salary.p3Performance <= 0) {
            return 0;
        }
        double ratio = Math.min(1d, Math.max(0d, achievementPct / 100d));
        return salary.p3Performance * ratio;
    }

    public static double estimatedTotalComp(Salary3P salary, double achievementPct, double actualRevenue, double commissionPct) {
        return salary.fixedMonthly()
                + p3Earned(salary, achievementPct)
                + actualRevenue * commissionPct / 100d;
    }

    public static double contributionMarginPct(double cogsPct, double riskPct, double commissionPct) {
        return 100d - cogsPct - riskPct - commissionPct;
    }

    public static double netProfitAfterTax(double netProfitBeforeTax, double taxPct) {
        if (netProfitBeforeTax <= 0) {
            return netProfitBeforeTax;
        }
        return netProfitBeforeTax * (1d - taxPct / 100d);
    }

    public static double corporateTaxAmount(double netProfitBeforeTax, double taxPct) {
        if (netProfitBeforeTax <= 0) {
            return 0;
        }
        return netProfitBeforeTax * taxPct / 100d;
    }
}
