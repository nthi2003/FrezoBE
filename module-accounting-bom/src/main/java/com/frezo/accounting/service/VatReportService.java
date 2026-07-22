package com.frezo.accounting.service;

import com.frezo.accounting.dto.response.VatReportResponse;

public interface VatReportService {
    VatReportResponse summarize(int year, int month);
}
