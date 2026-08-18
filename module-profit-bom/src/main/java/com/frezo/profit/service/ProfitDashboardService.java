package com.frezo.profit.service;

import com.frezo.profit.dto.response.ProfitDashboardResponse;
import com.frezo.profit.dto.response.ProfitSalesKpiResponse;

public interface ProfitDashboardService {

    ProfitDashboardResponse getDashboard(int year, int month, boolean persistSnapshot);

    ProfitSalesKpiResponse getSalesKpi(int year, int month);
}
