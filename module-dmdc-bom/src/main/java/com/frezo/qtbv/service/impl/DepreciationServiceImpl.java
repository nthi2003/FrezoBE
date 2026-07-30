package com.frezo.qtbv.service.impl;

import com.frezo.accounting.common.AccountingErrorCode;
import com.frezo.accounting.common.PeriodStatus;
import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.dto.request.JournalEntryRequest;
import com.frezo.accounting.dto.request.JournalLineRequest;
import com.frezo.accounting.dto.response.JournalEntryResponse;
import com.frezo.accounting.entity.FiscalPeriod;
import com.frezo.accounting.service.FiscalPeriodService;
import com.frezo.accounting.service.JournalService;
import com.frezo.common.exception.AppException;
import com.frezo.qtbv.depreciation.DepreciationCalculator;
import com.frezo.qtbv.depreciation.DepreciationConstants;
import com.frezo.qtbv.depreciation.DepreciationErrorCode;
import com.frezo.qtbv.dto.request.DepreciationScheduleRequest;
import com.frezo.qtbv.dto.response.DepreciationPostingResponse;
import com.frezo.qtbv.dto.response.DepreciationScheduleResponse;
import com.frezo.qtbv.entity.Asset;
import com.frezo.qtbv.entity.DepreciationPosting;
import com.frezo.qtbv.entity.DepreciationSchedule;
import com.frezo.qtbv.repository.AssetRepository;
import com.frezo.qtbv.repository.DepreciationPostingRepository;
import com.frezo.qtbv.repository.DepreciationScheduleRepository;
import com.frezo.qtbv.service.DepreciationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Depreciation — schedule + post GL. Skip asset DISPOSED. Idempotent DEP-YYYY-MM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepreciationServiceImpl implements DepreciationService {

    private static final String STATUS_DISPOSED = "DISPOSED";

    private final AssetRepository assetRepository;
    private final DepreciationScheduleRepository scheduleRepository;
    private final DepreciationPostingRepository postingRepository;
    private final JournalService journalService;
    private final FiscalPeriodService fiscalPeriodService;

    @Override
    @Transactional
    public DepreciationScheduleResponse generateSchedule(DepreciationScheduleRequest req) {
        Asset asset = assetRepository.findById(req.getAssetId())
                .filter(a -> Boolean.FALSE.equals(a.getIsDeleted()))
                .orElseThrow(() -> new AppException(DepreciationErrorCode.ASSET_NOT_FOUND, req.getAssetId()));

        if (asset.getPurchasePrice() == null || asset.getPurchasePrice().signum() <= 0) {
            throw new AppException(DepreciationErrorCode.ASSET_MISSING_PRICE, asset.getCode());
        }

        scheduleRepository.findByAssetIdAndIsDeletedFalse(asset.getId()).ifPresent(existing -> {
            throw new AppException(DepreciationErrorCode.SCHEDULE_EXISTS, asset.getCode());
        });

        String method = req.getMethod() != null ? req.getMethod() : DepreciationConstants.METHOD_STRAIGHT_LINE;
        if (!DepreciationConstants.METHOD_STRAIGHT_LINE.equals(method)
                && !DepreciationConstants.METHOD_DECLINING.equals(method)) {
            throw new AppException(DepreciationErrorCode.METHOD_INVALID, method);
        }

        int months = req.getMonths() != null ? req.getMonths() : 36;
        LocalDate start = req.getStartDate() != null ? req.getStartDate()
                : (asset.getPurchaseDate() != null ? asset.getPurchaseDate() : LocalDate.now());
        BigDecimal monthly = DepreciationCalculator.monthlyStraightLine(asset.getPurchasePrice(), months);

        DepreciationSchedule s = DepreciationSchedule.builder()
                .assetId(asset.getId())
                .method(method)
                .startDate(start)
                .months(months)
                .monthlyAmount(monthly)
                .remainingValue(asset.getPurchasePrice())
                .status(DepreciationConstants.SCHEDULE_ACTIVE)
                .build();
        return toResponse(scheduleRepository.save(s), asset);
    }

    @Override
    public List<DepreciationScheduleResponse> listSchedules(String assetId) {
        List<DepreciationSchedule> all;
        if (assetId != null && !assetId.isBlank()) {
            all = scheduleRepository.findByAssetIdAndIsDeletedFalse(assetId)
                    .map(List::of).orElseGet(List::of);
        } else {
            all = scheduleRepository.findAll().stream()
                    .filter(s -> Boolean.FALSE.equals(s.getIsDeleted()))
                    .sorted(Comparator.comparing(DepreciationSchedule::getCreatedDate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
        }
        return all.stream().map(s -> toResponse(s, null)).toList();
    }

    @Override
    public DepreciationPostingResponse previewPeriod(int year, int month) {
        List<DepreciationSchedule> eligible = eligibleSchedules(year, month);
        BigDecimal total = eligible.stream()
                .map(s -> s.getMonthlyAmount() != null ? s.getMonthlyAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return DepreciationPostingResponse.builder()
                .periodYear(year)
                .periodMonth(month)
                .totalAmount(total)
                .scheduleCount(eligible.size())
                .status("PREVIEW")
                .build();
    }

    @Override
    public List<DepreciationPostingResponse> listPostings(Integer year, Integer month) {
        List<DepreciationPosting> list;
        if (year != null && month != null) {
            list = postingRepository.findByPeriodYearAndPeriodMonthAndIsDeletedFalse(year, month);
            if (list.isEmpty()) {
                postingRepository.findByPeriodYearAndPeriodMonth(year, month).ifPresent(list::add);
            }
        } else {
            list = postingRepository.findByIsDeletedFalseOrderByPeriodYearDescPeriodMonthDesc();
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DepreciationPostingResponse postPeriod(int year, int month) {
        var existing = postingRepository.findByPeriodYearAndPeriodMonth(year, month);
        if (existing.isPresent()
                && DepreciationConstants.POSTING_POSTED.equals(existing.get().getStatus())) {
            log.info("[Depreciation] Kỳ {}/{} đã post trước đó → trả về entry cũ {}",
                    month, year, existing.get().getJournalEntryId());
            return toResponse(existing.get());
        }

        // BE-DEP-002: chặn sớm khi kỳ CLOSED/LOCKED (trước khi đụng schedule / JE)
        assertPeriodOpen(year, month);

        List<DepreciationSchedule> eligible = eligibleSchedules(year, month);
        if (eligible.isEmpty()) {
            throw new AppException(DepreciationErrorCode.NO_ACTIVE_SCHEDULE, year + "-" + month);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (DepreciationSchedule s : eligible) {
            BigDecimal monthly = s.getMonthlyAmount() != null ? s.getMonthlyAmount() : BigDecimal.ZERO;
            total = total.add(monthly);
            s.setRemainingValue(DepreciationCalculator.deductMonth(s.getRemainingValue(), monthly));
            int elapsed = DepreciationCalculator.monthsElapsed(s, year, month);
            if (elapsed >= s.getMonths()) {
                s.setStatus(DepreciationConstants.SCHEDULE_DONE);
                s.setRemainingValue(BigDecimal.ZERO);
            }
            scheduleRepository.save(s);
        }

        JournalEntryRequest req = buildJournalRequest(year, month, total, eligible.size());
        DepreciationPosting posting = existing.orElseGet(() -> DepreciationPosting.builder()
                .periodYear(year)
                .periodMonth(month)
                .build());
        posting.setTotalAmount(total);
        posting.setScheduleCount(eligible.size());
        posting.setStatus(DepreciationConstants.POSTING_FAILED);
        try {
            JournalEntryResponse response = journalService.createAndPost(req);
            posting.setJournalEntryId(response.getId());
            posting.setStatus(DepreciationConstants.POSTING_POSTED);
            posting.setErrorMessage(null);
            log.info("[Depreciation] Kỳ {}/{} → post GL {} (total {}, {} schedule)",
                    month, year, response.getCode(), total, eligible.size());
        } catch (Exception e) {
            posting.setErrorMessage(e.getMessage());
            log.error("[Depreciation] Post GL kỳ {}/{} thất bại: {}", month, year, e.getMessage(), e);
            postingRepository.save(posting);
            throw e;
        }
        return toResponse(postingRepository.save(posting));
    }

    private void assertPeriodOpen(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        FiscalPeriod period = fiscalPeriodService.findOrCreateByDate(ym.atEndOfMonth());
        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new AppException(AccountingErrorCode.PERIOD_CLOSED, month + "/" + year);
        }
    }

    private List<DepreciationSchedule> eligibleSchedules(int year, int month) {
        List<DepreciationSchedule> active = scheduleRepository
                .findByStatusAndIsDeletedFalse(DepreciationConstants.SCHEDULE_ACTIVE);
        return active.stream()
                .filter(s -> DepreciationCalculator.shouldPost(s, year, month))
                .filter(s -> {
                    Asset asset = assetRepository.findById(s.getAssetId()).orElse(null);
                    if (asset == null || Boolean.TRUE.equals(asset.getIsDeleted())) return false;
                    return !STATUS_DISPOSED.equalsIgnoreCase(asset.getStatus());
                })
                .toList();
    }

    private JournalEntryRequest buildJournalRequest(int year, int month, BigDecimal total, int count) {
        JournalLineRequest debit = new JournalLineRequest();
        debit.setAccountCode(DepreciationConstants.DEFAULT_DEPRECIATION_EXPENSE);
        debit.setDebit(total);
        debit.setCredit(BigDecimal.ZERO);
        debit.setDescription(String.format("Khấu hao TSCĐ kỳ %02d/%d (%d TS)", month, year, count));

        JournalLineRequest credit = new JournalLineRequest();
        credit.setAccountCode(DepreciationConstants.DEFAULT_ACCUMULATED_DEPRECIATION);
        credit.setDebit(BigDecimal.ZERO);
        credit.setCredit(total);
        credit.setDescription("Hao mòn luỹ kế");

        List<JournalLineRequest> lines = new ArrayList<>(List.of(debit, credit));

        JournalEntryRequest req = new JournalEntryRequest();
        YearMonth ym = YearMonth.of(year, month);
        req.setPostingDate(ym.atEndOfMonth());
        req.setDocumentDate(ym.atEndOfMonth());
        req.setDescription(String.format("Hạch toán khấu hao TSCĐ kỳ %02d/%d", month, year));
        req.setSourceType(PostingSource.DEPRECIATION);
        req.setSourceId(year + "-" + month);
        req.setIdempotencyKey(DepreciationConstants.idempotencyKey(year, month));
        req.setLines(lines);
        return req;
    }

    private DepreciationScheduleResponse toResponse(DepreciationSchedule s, Asset assetCached) {
        Asset asset = assetCached != null ? assetCached
                : assetRepository.findById(s.getAssetId()).orElse(null);
        return DepreciationScheduleResponse.builder()
                .id(s.getId())
                .assetId(s.getAssetId())
                .assetCode(asset != null ? asset.getCode() : null)
                .assetName(asset != null ? asset.getName() : null)
                .method(s.getMethod())
                .startDate(s.getStartDate())
                .months(s.getMonths())
                .purchasePrice(asset != null ? asset.getPurchasePrice() : null)
                .monthlyAmount(s.getMonthlyAmount())
                .remainingValue(s.getRemainingValue())
                .status(s.getStatus())
                .build();
    }

    private DepreciationPostingResponse toResponse(DepreciationPosting p) {
        return DepreciationPostingResponse.builder()
                .id(p.getId())
                .periodYear(p.getPeriodYear())
                .periodMonth(p.getPeriodMonth())
                .totalAmount(p.getTotalAmount())
                .scheduleCount(p.getScheduleCount())
                .journalEntryId(p.getJournalEntryId())
                .status(p.getStatus())
                .errorMessage(p.getErrorMessage())
                .build();
    }
}
