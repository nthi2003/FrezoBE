package com.frezo.accounting.service;

import com.frezo.accounting.dto.response.AccountingDashboardResponse;

public interface AccountingDashboardService {

    AccountingDashboardResponse getSummary(int year, int month);
}
