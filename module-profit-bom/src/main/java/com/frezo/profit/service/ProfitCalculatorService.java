package com.frezo.profit.service;

import com.frezo.profit.dto.request.ProfitCalculateRequest;
import com.frezo.profit.dto.response.ProfitCalculateResponse;

public interface ProfitCalculatorService {

    ProfitCalculateResponse calculate(ProfitCalculateRequest req);
}
