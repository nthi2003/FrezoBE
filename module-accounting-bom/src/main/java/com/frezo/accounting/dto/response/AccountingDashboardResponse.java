package com.frezo.accounting.dto.response;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Value
@Builder
public class AccountingDashboardResponse {
    int year;
    int month;
    String periodLabel;
    String asOf;

    BigDecimal totalRevenue;
    BigDecimal totalExpense;
    BigDecimal netProfit;
    BigDecimal accountsReceivable;
    BigDecimal accountsPayable;
    String accountsReceivableHint;
    String accountsPayableHint;

    /** % thay đổi so với tháng trước (null nếu không tính được). */
    Double revenueChangePct;
    Double netProfitChangePct;
    Double arChangePct;
    Double apChangePct;

    List<MonthlyTrendPoint> cashFlowTrend;
    List<RevenueCategoryPoint> revenueByCategory;
    List<RecentTransactionPoint> recentTransactions;

    int journalCountPosted;
    int journalCountDraft;
    boolean trialBalanceBalanced;

    @Value
    @Builder
    public static class MonthlyTrendPoint {
        String label;
        int year;
        int month;
        BigDecimal cashIn;
        BigDecimal cashOut;
    }

    @Value
    @Builder
    public static class RevenueCategoryPoint {
        String label;
        String accountCode;
        BigDecimal amount;
    }

    @Value
    @Builder
    public static class RecentTransactionPoint {
        String id;
        String code;
        String postingDate;
        String description;
        String category;
        BigDecimal amount;
        String status;
        String statusLabel;
        String sourceType;
    }
}
