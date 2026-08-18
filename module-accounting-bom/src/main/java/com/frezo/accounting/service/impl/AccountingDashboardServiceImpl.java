package com.frezo.accounting.service.impl;

import com.frezo.accounting.common.AccountType;
import com.frezo.accounting.common.JournalStatus;
import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.dto.response.AccountingDashboardResponse;
import com.frezo.accounting.dto.response.IncomeStatementResponse;
import com.frezo.accounting.dto.response.TrialBalanceRowResponse;
import com.frezo.accounting.entity.Account;
import com.frezo.accounting.entity.JournalEntry;
import com.frezo.accounting.repository.AccountRepository;
import com.frezo.accounting.repository.JournalEntryRepository;
import com.frezo.accounting.service.AccountingDashboardService;
import com.frezo.accounting.service.FinancialReportService;
import com.frezo.accounting.service.GLService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AccountingDashboardServiceImpl implements AccountingDashboardService {

    private static final DateTimeFormatter AS_OF_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));

    private final FinancialReportService financialReportService;
    private final GLService glService;
    private final JournalEntryRepository entryRepo;
    private final AccountRepository accountRepo;

    @Override
    @Transactional(readOnly = true)
    public AccountingDashboardResponse getSummary(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        YearMonth prevYm = ym.minusMonths(1);
        LocalDate prevFrom = prevYm.atDay(1);
        LocalDate prevTo = prevYm.atEndOfMonth();

        Map<String, Account> accountsByCode = loadAccountsByCode();

        IncomeStatementResponse currentIs = financialReportService.incomeStatement(from, to);
        IncomeStatementResponse prevIs = financialReportService.incomeStatement(prevFrom, prevTo);

        BigDecimal revenue = safe(currentIs.getTotalRevenue());
        BigDecimal expense = safe(currentIs.getTotalExpense());
        BigDecimal net = safe(currentIs.getNetIncome());

        BigDecimal prevRevenue = safe(prevIs.getTotalRevenue());
        BigDecimal prevNet = safe(prevIs.getNetIncome());

        List<TrialBalanceRowResponse> tb = glService.trialBalance(from, to);
        List<TrialBalanceRowResponse> prevTb = glService.trialBalance(prevFrom, prevTo);

        PartnerBalanceSummary ar = sumPartnerBalances(tb, accountsByCode, AccountType.ASSET);
        PartnerBalanceSummary arPrev = sumPartnerBalances(prevTb, accountsByCode, AccountType.ASSET);
        PartnerBalanceSummary ap = sumPartnerBalances(tb, accountsByCode, AccountType.LIABILITY);
        PartnerBalanceSummary apPrev = sumPartnerBalances(prevTb, accountsByCode, AccountType.LIABILITY);

        List<AccountingDashboardResponse.MonthlyTrendPoint> trend = buildTrend(ym);
        List<AccountingDashboardResponse.RevenueCategoryPoint> revCats = buildRevenueCategories(currentIs);
        List<AccountingDashboardResponse.RecentTransactionPoint> recent = buildRecentTransactions();

        long posted = entryRepo.countByStatus(JournalStatus.POSTED);
        long draft = entryRepo.countByStatus(JournalStatus.DRAFT);
        boolean balanced = isTrialBalanceBalanced(tb);

        return AccountingDashboardResponse.builder()
                .year(year)
                .month(month)
                .periodLabel(String.format("Tháng %d/%d", month, year))
                .asOf(AS_OF_FMT.format(LocalDateTime.now()))
                .totalRevenue(revenue)
                .totalExpense(expense)
                .netProfit(net)
                .accountsReceivable(ar.total())
                .accountsPayable(ap.total())
                .accountsReceivableHint(ar.hint())
                .accountsPayableHint(ap.hint())
                .revenueChangePct(pctChange(revenue, prevRevenue))
                .netProfitChangePct(pctChange(net, prevNet))
                .arChangePct(pctChange(ar.total(), arPrev.total()))
                .apChangePct(pctChange(ap.total(), apPrev.total()))
                .cashFlowTrend(trend)
                .revenueByCategory(revCats)
                .recentTransactions(recent)
                .journalCountPosted((int) posted)
                .journalCountDraft((int) draft)
                .trialBalanceBalanced(balanced)
                .build();
    }

    private Map<String, Account> loadAccountsByCode() {
        Map<String, Account> map = new HashMap<>();
        for (Account a : accountRepo.findByIsDeletedFalseOrderByCodeAsc()) {
            map.put(a.getCode(), a);
        }
        return map;
    }

    private PartnerBalanceSummary sumPartnerBalances(
            List<TrialBalanceRowResponse> tb,
            Map<String, Account> accountsByCode,
            AccountType type) {
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        for (TrialBalanceRowResponse row : tb) {
            Account acc = accountsByCode.get(row.getAccountCode());
            if (acc == null || acc.getType() != type || !Boolean.TRUE.equals(acc.getRequiresPartner())) {
                continue;
            }
            BigDecimal bal = closingBalance(row, type);
            if (bal.signum() == 0) continue;
            sum = sum.add(bal);
            count++;
        }
        String hint = count == 0
                ? String.format("Chưa có số dư TK %s có đối tượng", type.getDisplayName())
                : String.format("Tổng %d TK %s có đối tượng", count, type.getDisplayName());
        return new PartnerBalanceSummary(sum, hint);
    }

    private List<AccountingDashboardResponse.MonthlyTrendPoint> buildTrend(YearMonth endYm) {
        List<AccountingDashboardResponse.MonthlyTrendPoint> out = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = endYm.minusMonths(i);
            LocalDate from = ym.atDay(1);
            LocalDate to = ym.atEndOfMonth();
            IncomeStatementResponse is = financialReportService.incomeStatement(from, to);
            out.add(AccountingDashboardResponse.MonthlyTrendPoint.builder()
                    .label(String.format("T%d/%d", ym.getMonthValue(), ym.getYear()))
                    .year(ym.getYear())
                    .month(ym.getMonthValue())
                    .cashIn(safe(is.getTotalRevenue()))
                    .cashOut(safe(is.getTotalExpense()))
                    .build());
        }
        return out;
    }

    private List<AccountingDashboardResponse.RevenueCategoryPoint> buildRevenueCategories(
            IncomeStatementResponse is) {
        if (is.getRevenues() == null) return List.of();
        return is.getRevenues().stream()
                .sorted(Comparator.comparing(
                        (IncomeStatementResponse.ReportLine l) -> safe(l.getAmount())).reversed())
                .limit(6)
                .map(l -> AccountingDashboardResponse.RevenueCategoryPoint.builder()
                        .label(l.getAccountName() != null ? l.getAccountName() : l.getAccountCode())
                        .accountCode(l.getAccountCode())
                        .amount(safe(l.getAmount()))
                        .build())
                .toList();
    }

    private List<AccountingDashboardResponse.RecentTransactionPoint> buildRecentTransactions() {
        List<JournalEntry> entries = entryRepo.findByStatusOrderByPostingDateDesc(
                JournalStatus.POSTED, PageRequest.of(0, 12));
        List<AccountingDashboardResponse.RecentTransactionPoint> out = new ArrayList<>();
        for (JournalEntry e : entries) {
            BigDecimal amt = e.getTotalDebit() != null ? e.getTotalDebit() : BigDecimal.ZERO;
            PostingSource src = e.getSourceType();
            out.add(AccountingDashboardResponse.RecentTransactionPoint.builder()
                    .id(e.getId())
                    .code(e.getCode())
                    .postingDate(e.getPostingDate() != null ? e.getPostingDate().toString() : null)
                    .description(e.getDescription())
                    .category(src != null ? src.getDisplayName() : "Khác")
                    .amount(amt)
                    .status(JournalStatus.POSTED.name())
                    .statusLabel(JournalStatus.POSTED.getDisplayName())
                    .sourceType(src != null ? src.name() : null)
                    .build());
        }
        return out;
    }

    private static BigDecimal closingBalance(TrialBalanceRowResponse row, AccountType type) {
        if (type == AccountType.ASSET || type == AccountType.EXPENSE) {
            return safe(row.getClosingDebit()).subtract(safe(row.getClosingCredit())).max(BigDecimal.ZERO);
        }
        return safe(row.getClosingCredit()).subtract(safe(row.getClosingDebit())).max(BigDecimal.ZERO);
    }

    private static boolean isTrialBalanceBalanced(List<TrialBalanceRowResponse> tb) {
        BigDecimal dr = BigDecimal.ZERO;
        BigDecimal cr = BigDecimal.ZERO;
        for (TrialBalanceRowResponse row : tb) {
            dr = dr.add(safe(row.getPeriodDebit()));
            cr = cr.add(safe(row.getPeriodCredit()));
        }
        return dr.subtract(cr).abs().compareTo(new BigDecimal("0.01")) <= 0;
    }

    private static Double pctChange(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return null;
        return current.subtract(previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(previous.abs(), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private record PartnerBalanceSummary(BigDecimal total, String hint) {}
}
