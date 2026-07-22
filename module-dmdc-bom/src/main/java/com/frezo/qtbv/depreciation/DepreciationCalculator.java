package com.frezo.qtbv.depreciation;

import com.frezo.qtbv.entity.DepreciationSchedule;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helper stateless — không phải Spring bean, chỉ chứa toán khấu hao thuần.
 * Tách khỏi service để test dễ và giữ service ≤5 deps.
 */
public final class DepreciationCalculator {

    private DepreciationCalculator() {}

    /**
     * Straight-line: monthly = purchasePrice / months, làm tròn 2 chữ số thập phân theo HALF_UP.
     */
    public static BigDecimal monthlyStraightLine(BigDecimal purchasePrice, int months) {
        if (purchasePrice == null || months <= 0) return BigDecimal.ZERO;
        return purchasePrice.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }

    /**
     * Số tháng đã trôi qua kể từ startDate của schedule tới kỳ {@code postingYear/postingMonth}
     * (bao gồm cả tháng bắt đầu). Trả về tối đa {@code schedule.months}.
     */
    public static int monthsElapsed(DepreciationSchedule schedule, int postingYear, int postingMonth) {
        int startYear = schedule.getStartDate().getYear();
        int startMonth = schedule.getStartDate().getMonthValue();
        int diff = (postingYear - startYear) * 12 + (postingMonth - startMonth) + 1;
        if (diff < 0) return 0;
        return Math.min(diff, schedule.getMonths());
    }

    /**
     * Kiểm tra schedule có nên post trong kỳ {@code year/month} không:
     * status ACTIVE + start ≤ kỳ + chưa đủ tháng.
     */
    public static boolean shouldPost(DepreciationSchedule s, int year, int month) {
        if (!DepreciationConstants.SCHEDULE_ACTIVE.equalsIgnoreCase(s.getStatus())) return false;
        int elapsed = monthsElapsed(s, year, month);
        return elapsed > 0 && elapsed <= s.getMonths();
    }

    /**
     * Trừ 1 tháng vào remainingValue, đảm bảo không âm.
     */
    public static BigDecimal deductMonth(BigDecimal remaining, BigDecimal monthlyAmount) {
        if (remaining == null) return BigDecimal.ZERO;
        BigDecimal next = remaining.subtract(monthlyAmount != null ? monthlyAmount : BigDecimal.ZERO);
        return next.signum() < 0 ? BigDecimal.ZERO : next;
    }
}
