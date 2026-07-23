package com.frezo.accounting.service.impl;

import com.frezo.accounting.common.AccountingErrorCode;
import com.frezo.accounting.common.PeriodStatus;
import com.frezo.accounting.dto.response.FiscalPeriodResponse;
import com.frezo.accounting.entity.FiscalPeriod;
import com.frezo.accounting.entity.FiscalYear;
import com.frezo.accounting.repository.FiscalPeriodRepository;
import com.frezo.accounting.repository.FiscalYearRepository;
import com.frezo.accounting.service.FiscalPeriodService;
import com.frezo.common.exception.AppException;
import com.frezo.common.helper.SystemUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FiscalPeriodServiceImpl implements FiscalPeriodService {

    private final FiscalYearRepository yearRepo;
    private final FiscalPeriodRepository periodRepo;

    @Override
    @Transactional
    public FiscalPeriodResponse ensureYear(int year) {
        FiscalYear fy = yearRepo.findByCode(String.valueOf(year)).orElseGet(() -> {
            FiscalYear ny = FiscalYear.builder()
                    .code(String.valueOf(year))
                    .startDate(LocalDate.of(year, 1, 1))
                    .endDate(LocalDate.of(year, 12, 31))
                    .closed(false)
                    .build();
            ny.setIsDeleted(false);
            return yearRepo.save(ny);
        });

        // Sinh 12 kỳ
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            if (periodRepo.findByMonthAndYear(month, year).isPresent()) continue;
            YearMonth ym = YearMonth.of(year, month);
            FiscalPeriod p = FiscalPeriod.builder()
                    .fiscalYearId(fy.getId())
                    .month(month)
                    .year(year)
                    .startDate(ym.atDay(1))
                    .endDate(ym.atEndOfMonth())
                    .status(PeriodStatus.OPEN)
                    .build();
            p.setIsDeleted(false);
            periodRepo.save(p);
        }
        // Trả kỳ hiện tại nếu năm current, ngược lại kỳ 1
        int mnow = LocalDate.now().getYear() == year ? LocalDate.now().getMonthValue() : 1;
        return toResponse(periodRepo.findByMonthAndYear(mnow, year).orElseThrow());
    }

    @Override
    @Transactional
    public FiscalPeriod findOrCreateByDate(LocalDate date) {
        return periodRepo.findByMonthAndYear(date.getMonthValue(), date.getYear())
                .orElseGet(() -> {
                    ensureYear(date.getYear());
                    return periodRepo.findByMonthAndYear(date.getMonthValue(), date.getYear())
                            .orElseThrow(() -> new AppException(AccountingErrorCode.PERIOD_NOT_FOUND, date));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public FiscalPeriod getRequired(String periodId) {
        return periodRepo.findById(periodId)
                .orElseThrow(() -> new AppException(AccountingErrorCode.PERIOD_NOT_FOUND, periodId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<FiscalPeriodResponse> listByYear(int year) {
        return periodRepo.findByYearOrderByMonthAsc(year).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public FiscalPeriodResponse closePeriod(String periodId) {
        FiscalPeriod p = periodRepo.findById(periodId)
                .orElseThrow(() -> new AppException(AccountingErrorCode.PERIOD_NOT_FOUND, periodId));
        if (p.getStatus() == PeriodStatus.LOCKED) {
            throw new AppException(AccountingErrorCode.PERIOD_LOCKED);
        }
        if (p.getStatus() == PeriodStatus.CLOSED) {
            throw new AppException(AccountingErrorCode.PERIOD_ALREADY_CLOSED);
        }
        p.setStatus(PeriodStatus.CLOSED);
        p.setClosedAt(LocalDateTime.now());
        p.setClosedBy(SystemUtils.getCurrentUsername());
        return toResponse(periodRepo.save(p));
    }

    @Override
    @Transactional
    public FiscalPeriodResponse reopenPeriod(String periodId) {
        FiscalPeriod p = periodRepo.findById(periodId)
                .orElseThrow(() -> new AppException(AccountingErrorCode.PERIOD_NOT_FOUND, periodId));
        if (p.getStatus() == PeriodStatus.LOCKED) {
            throw new AppException(AccountingErrorCode.PERIOD_LOCKED);
        }
        p.setStatus(PeriodStatus.OPEN);
        p.setClosedAt(null);
        p.setClosedBy(null);
        return toResponse(periodRepo.save(p));
    }

    private FiscalPeriodResponse toResponse(FiscalPeriod p) {
        return FiscalPeriodResponse.builder()
                .id(p.getId())
                .fiscalYearId(p.getFiscalYearId())
                .month(p.getMonth())
                .year(p.getYear())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .status(p.getStatus())
                .closedAt(p.getClosedAt())
                .closedBy(p.getClosedBy())
                .build();
    }
}
