package com.frezo.accounting.service.impl;

import com.frezo.accounting.dto.response.VatReportResponse;
import com.frezo.accounting.repository.JournalEntryLineRepository;
import com.frezo.accounting.service.VatReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * VAT stub TT133 — tổng hợp thô từ journal lines TK 133 (VAT đầu vào) / 3331 (VAT đầu ra).
 */
@Service
@RequiredArgsConstructor
public class VatReportServiceImpl implements VatReportService {

    private final JournalEntryLineRepository journalEntryLineRepository;

    @Override
    public VatReportResponse summarize(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        // Dùng aggregateByAccount rồi filter code — đơn giản, không thêm query phức tạp.
        List<Object[]> rows = journalEntryLineRepository.aggregateByAccount(from, to);
        BigDecimal inputVat = BigDecimal.ZERO;
        BigDecimal outputVat = BigDecimal.ZERO;
        for (Object[] row : rows) {
            String code = row[1] != null ? row[1].toString() : "";
            BigDecimal debit = row[2] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            BigDecimal credit = row[3] instanceof BigDecimal b ? b : BigDecimal.ZERO;
            if (code.startsWith("133")) {
                inputVat = inputVat.add(debit);
            } else if (code.startsWith("3331") || code.startsWith("33311")) {
                outputVat = outputVat.add(credit);
            }
        }

        return VatReportResponse.builder()
                .year(year)
                .month(month)
                .inputVat(inputVat.doubleValue())
                .outputVat(outputVat.doubleValue())
                .netVat(outputVat.subtract(inputVat).doubleValue())
                .standard("TT133")
                .note("Stub summary từ journal lines 133*/3331* — chưa đủ form tờ khai GTGT")
                .build();
    }
}
