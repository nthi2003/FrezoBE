package com.frezo.qtbv.depreciation;

import com.frezo.qtbv.entity.DepreciationSchedule;

import java.math.BigDecimal;
import java.math.RoundingMode;


public final class DepreciationCalculator {

    private DepreciationCalculator() {}


    public static BigDecimal monthlyStraightLine(BigDecimal purchasePrice, int months) {
        if (purchasePrice == null || months <= 0) return BigDecimal.ZERO;
        return purchasePrice.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }


    public static int monthsElapsed(DepreciationSchedule schedule, int postingYear, int postingMonth) {
        int startYear = schedule.getStartDate().getYear();
        int startMonth = schedule.getStartDate().getMonthValue();
        int diff = (postingYear - startYear) * 12 + (postingMonth - startMonth) + 1;
        if (diff < 0) return 0;
        return Math.min(diff, schedule.getMonths());
    }


    public static boolean shouldPost(DepreciationSchedule s, int year, int month) {
        if (!DepreciationConstants.SCHEDULE_ACTIVE.equalsIgnoreCase(s.getStatus())) return false;
        int elapsed = monthsElapsed(s, year, month);
        return elapsed > 0 && elapsed <= s.getMonths();
    }


    public static BigDecimal deductMonth(BigDecimal remaining, BigDecimal monthlyAmount) {
        if (remaining == null) return BigDecimal.ZERO;
        BigDecimal next = remaining.subtract(monthlyAmount != null ? monthlyAmount : BigDecimal.ZERO);
        return next.signum() < 0 ? BigDecimal.ZERO : next;
    }
}
