package com.frezo.accounting.service;

import com.frezo.accounting.dto.response.BalanceSheetResponse;
import com.frezo.accounting.dto.response.IncomeStatementResponse;

import java.time.LocalDate;

public interface FinancialReportService {

    BalanceSheetResponse balanceSheet(LocalDate from, LocalDate to);

    IncomeStatementResponse incomeStatement(LocalDate from, LocalDate to);
}
