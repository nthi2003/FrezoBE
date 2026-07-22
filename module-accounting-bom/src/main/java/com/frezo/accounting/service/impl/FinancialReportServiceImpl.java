package com.frezo.accounting.service.impl;

import com.frezo.accounting.common.AccountType;
import com.frezo.accounting.dto.response.BalanceSheetResponse;
import com.frezo.accounting.dto.response.IncomeStatementResponse;
import com.frezo.accounting.dto.response.TrialBalanceRowResponse;
import com.frezo.accounting.entity.Account;
import com.frezo.accounting.repository.AccountRepository;
import com.frezo.accounting.service.FinancialReportService;
import com.frezo.accounting.service.GLService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stub BCTC TT133 — map AccountType từ trial-balance (reuse GLService).
 * Không phải full VAS.
 */
@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private final GLService glService;
    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public BalanceSheetResponse balanceSheet(LocalDate from, LocalDate to) {
        Map<String, Account> byCode = loadAccounts();
        List<TrialBalanceRowResponse> tb = glService.trialBalance(from, to);

        List<BalanceSheetResponse.ReportLine> assets = new ArrayList<>();
        List<BalanceSheetResponse.ReportLine> liabilities = new ArrayList<>();
        List<BalanceSheetResponse.ReportLine> equity = new ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiab = BigDecimal.ZERO;
        BigDecimal totalEq = BigDecimal.ZERO;

        for (TrialBalanceRowResponse row : tb) {
            Account acc = byCode.get(row.getAccountCode());
            if (acc == null || acc.getType() == null) continue;
            BigDecimal amt = closingBalance(row, acc.getType());
            if (amt.signum() == 0) continue;
            BalanceSheetResponse.ReportLine line = BalanceSheetResponse.ReportLine.builder()
                    .accountCode(row.getAccountCode())
                    .accountName(row.getAccountName())
                    .amount(amt)
                    .build();
            switch (acc.getType()) {
                case ASSET -> {
                    assets.add(line);
                    totalAssets = totalAssets.add(amt);
                }
                case LIABILITY -> {
                    liabilities.add(line);
                    totalLiab = totalLiab.add(amt);
                }
                case EQUITY -> {
                    equity.add(line);
                    totalEq = totalEq.add(amt);
                }
                default -> { /* skip P&L */ }
            }
        }

        return BalanceSheetResponse.builder()
                .from(from.toString())
                .to(to.toString())
                .totalAssets(totalAssets)
                .totalLiabilities(totalLiab)
                .totalEquity(totalEq)
                .assets(assets)
                .liabilities(liabilities)
                .equity(equity)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public IncomeStatementResponse incomeStatement(LocalDate from, LocalDate to) {
        Map<String, Account> byCode = loadAccounts();
        List<TrialBalanceRowResponse> tb = glService.trialBalance(from, to);

        List<IncomeStatementResponse.ReportLine> revenues = new ArrayList<>();
        List<IncomeStatementResponse.ReportLine> expenses = new ArrayList<>();
        BigDecimal totalRev = BigDecimal.ZERO;
        BigDecimal totalExp = BigDecimal.ZERO;

        for (TrialBalanceRowResponse row : tb) {
            Account acc = byCode.get(row.getAccountCode());
            if (acc == null || acc.getType() == null) continue;
            if (acc.getType() == AccountType.REVENUE) {
                BigDecimal rev = safe(row.getPeriodCredit()).subtract(safe(row.getPeriodDebit()));
                if (rev.signum() <= 0) continue;
                revenues.add(IncomeStatementResponse.ReportLine.builder()
                        .accountCode(row.getAccountCode())
                        .accountName(row.getAccountName())
                        .amount(rev)
                        .build());
                totalRev = totalRev.add(rev);
            } else if (acc.getType() == AccountType.EXPENSE) {
                BigDecimal exp = safe(row.getPeriodDebit()).subtract(safe(row.getPeriodCredit()));
                if (exp.signum() <= 0) continue;
                expenses.add(IncomeStatementResponse.ReportLine.builder()
                        .accountCode(row.getAccountCode())
                        .accountName(row.getAccountName())
                        .amount(exp)
                        .build());
                totalExp = totalExp.add(exp);
            }
        }

        return IncomeStatementResponse.builder()
                .from(from.toString())
                .to(to.toString())
                .totalRevenue(totalRev)
                .totalExpense(totalExp)
                .netIncome(totalRev.subtract(totalExp))
                .revenues(revenues)
                .expenses(expenses)
                .build();
    }

    private BigDecimal closingBalance(TrialBalanceRowResponse row, AccountType type) {
        // Debit nature (ASSET/EXPENSE): closing debit - credit
        // Credit nature (LIABILITY/EQUITY/REVENUE): closing credit - debit
        if (type == AccountType.ASSET || type == AccountType.EXPENSE) {
            return safe(row.getClosingDebit()).subtract(safe(row.getClosingCredit()));
        }
        return safe(row.getClosingCredit()).subtract(safe(row.getClosingDebit()));
    }

    private Map<String, Account> loadAccounts() {
        Map<String, Account> map = new HashMap<>();
        for (Account a : accountRepository.findByIsDeletedFalseOrderByCodeAsc()) {
            map.put(a.getCode(), a);
        }
        return map;
    }

    private static BigDecimal safe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
