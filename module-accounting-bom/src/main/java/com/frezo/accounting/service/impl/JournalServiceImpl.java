package com.frezo.accounting.service.impl;

import com.frezo.accounting.common.AccountingErrorCode;
import com.frezo.accounting.common.JournalStatus;
import com.frezo.accounting.common.PeriodStatus;
import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.dto.request.JournalEntryRequest;
import com.frezo.accounting.dto.request.JournalLineRequest;
import com.frezo.accounting.dto.response.JournalEntryResponse;
import com.frezo.accounting.dto.response.JournalLineResponse;
import com.frezo.accounting.entity.Account;
import com.frezo.accounting.entity.FiscalPeriod;
import com.frezo.accounting.entity.JournalEntry;
import com.frezo.accounting.entity.JournalEntryLine;
import com.frezo.accounting.repository.AccountRepository;
import com.frezo.accounting.repository.JournalEntryLineRepository;
import com.frezo.accounting.repository.JournalEntryRepository;
import com.frezo.accounting.service.FiscalPeriodService;
import com.frezo.accounting.service.JournalService;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JournalServiceImpl implements JournalService {

    private final JournalEntryRepository entryRepo;
    private final JournalEntryLineRepository lineRepo;
    private final AccountRepository accountRepo;
    private final FiscalPeriodService periodService;

    @Override
    @Transactional
    public JournalEntryResponse createDraft(JournalEntryRequest req) {
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            var existing = entryRepo.findByIdempotencyKey(req.getIdempotencyKey());
            if (existing.isPresent()) {
                return withLines(existing.get());
            }
        }
        return persist(req, JournalStatus.DRAFT);
    }

    @Override
    @Transactional
    public JournalEntryResponse createAndPost(JournalEntryRequest req) {
        // Idempotency check
        if (req.getIdempotencyKey() != null && !req.getIdempotencyKey().isBlank()) {
            var existing = entryRepo.findByIdempotencyKey(req.getIdempotencyKey());
            if (existing.isPresent()) {
                return withLines(existing.get());
            }
        }
        return persist(req, JournalStatus.POSTED);
    }

    @Override
    @Transactional
    public JournalEntryResponse post(String id) {
        JournalEntry e = entryRepo.findById(id)
                .orElseThrow(() -> new AppException(AccountingErrorCode.JOURNAL_NOT_FOUND, id));
        if (e.getStatus() == JournalStatus.POSTED) {
            throw new AppException(AccountingErrorCode.JOURNAL_ALREADY_POSTED, id);
        }
        if (e.getStatus() == JournalStatus.REVERSED) {
            throw new AppException(AccountingErrorCode.JOURNAL_ALREADY_REVERSED, id);
        }
        assertPeriodStatus(periodService.getRequired(e.getPeriodId()));
        e.setStatus(JournalStatus.POSTED);
        e.setPostedAt(LocalDateTime.now());
        entryRepo.save(e);
        return withLines(e);
    }

    @Override
    @Transactional
    public JournalEntryResponse reverse(String id, String reason) {
        JournalEntry original = entryRepo.findById(id)
                .orElseThrow(() -> new AppException(AccountingErrorCode.JOURNAL_NOT_FOUND, id));
        if (original.getStatus() != JournalStatus.POSTED) {
            throw new AppException(AccountingErrorCode.JOURNAL_ALREADY_REVERSED, id);
        }
        FiscalPeriod reversalPeriod = periodService.findOrCreateByDate(LocalDate.now());
        assertPeriodStatus(reversalPeriod);
        List<JournalEntryLine> lines = lineRepo.findByJournalEntryIdOrderByLineNoAsc(id);

        JournalEntry reversal = JournalEntry.builder()
                .code(nextCode(LocalDate.now()))
                .postingDate(LocalDate.now())
                .documentDate(original.getPostingDate())
                .periodId(reversalPeriod.getId())
                .description("Đảo chứng từ " + original.getCode()
                        + (reason == null ? "" : " — " + reason))
                .sourceType(PostingSource.REVERSAL)
                .sourceId(original.getId())
                .status(JournalStatus.POSTED)
                .totalDebit(original.getTotalCredit())
                .totalCredit(original.getTotalDebit())
                .postedAt(LocalDateTime.now())
                .reversalOfId(original.getId())
                .build();
        reversal.setIsDeleted(false);
        entryRepo.save(reversal);

        int lineNo = 1;
        for (JournalEntryLine ol : lines) {
            JournalEntryLine rl = JournalEntryLine.builder()
                    .journalEntryId(reversal.getId())
                    .lineNo(lineNo++)
                    .accountId(ol.getAccountId())
                    .accountCode(ol.getAccountCode())
                    .debit(ol.getCredit())    // swap
                    .credit(ol.getDebit())
                    .description("Đảo: " + (ol.getDescription() != null ? ol.getDescription() : ""))
                    .departmentId(ol.getDepartmentId())
                    .partnerType(ol.getPartnerType())
                    .partnerId(ol.getPartnerId())
                    .partnerName(ol.getPartnerName())
                    .projectId(ol.getProjectId())
                    .build();
            rl.setIsDeleted(false);
            lineRepo.save(rl);
        }

        // Đánh dấu original đã bị đảo
        original.setStatus(JournalStatus.REVERSED);
        entryRepo.save(original);

        return withLines(reversal);
    }

    @Override
    @Transactional(readOnly = true)
    public JournalEntryResponse getById(String id) {
        JournalEntry e = entryRepo.findById(id)
                .orElseThrow(() -> new AppException(AccountingErrorCode.JOURNAL_NOT_FOUND, id));
        return withLines(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntryResponse> listByPeriod(String periodId) {
        return entryRepo.findByPeriodIdOrderByPostingDateDesc(periodId).stream()
                .map(this::withLines).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<JournalEntryResponse> listBySource(PostingSource source, String sourceId) {
        return entryRepo.findBySourceTypeAndSourceIdOrderByPostingDateDesc(source, sourceId).stream()
                .map(this::withLines).toList();
    }

    // ------------------------ Helpers ------------------------

    private JournalEntryResponse persist(JournalEntryRequest req, JournalStatus targetStatus) {
        if (req.getLines() == null || req.getLines().size() < 2) {
            throw new AppException(AccountingErrorCode.JOURNAL_EMPTY_LINES);
        }
        LocalDate postingDate = req.getPostingDate();
        FiscalPeriod period = periodService.findOrCreateByDate(postingDate);
        if (targetStatus == JournalStatus.POSTED) {
            assertPeriodStatus(period);
        }

        // Validate + total
        BigDecimal totalDr = BigDecimal.ZERO;
        BigDecimal totalCr = BigDecimal.ZERO;
        Map<String, Account> accCache = new HashMap<>();

        for (JournalLineRequest l : req.getLines()) {
            BigDecimal dr = l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO;
            BigDecimal cr = l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO;
            boolean bothZero = dr.signum() == 0 && cr.signum() == 0;
            boolean bothNonZero = dr.signum() > 0 && cr.signum() > 0;
            if (bothZero || bothNonZero) {
                throw new AppException(AccountingErrorCode.JOURNAL_LINE_INVALID,
                        l.getAccountCode() + " dr=" + dr + " cr=" + cr);
            }
            Account acc = accCache.computeIfAbsent(l.getAccountCode(),
                    code -> accountRepo.findByCodeAndIsDeletedFalse(code)
                            .orElseThrow(() -> new AppException(AccountingErrorCode.ACCOUNT_NOT_FOUND, code)));
            if (Boolean.FALSE.equals(acc.getPostable())) {
                throw new AppException(AccountingErrorCode.ACCOUNT_NOT_POSTABLE, acc.getCode());
            }
            if (Boolean.TRUE.equals(acc.getRequiresPartner())
                    && (l.getPartnerId() == null || l.getPartnerId().isBlank())) {
                throw new AppException(AccountingErrorCode.ACCOUNT_REQUIRES_PARTNER, acc.getCode());
            }
            totalDr = totalDr.add(dr);
            totalCr = totalCr.add(cr);
        }
        if (totalDr.compareTo(totalCr) != 0) {
            throw new AppException(AccountingErrorCode.JOURNAL_UNBALANCED,
                    "Nợ=" + totalDr + " Có=" + totalCr);
        }
        if (totalDr.signum() == 0) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Chứng từ không có số tiền");
        }

        JournalEntry entry = JournalEntry.builder()
                .code(nextCode(postingDate))
                .postingDate(postingDate)
                .documentDate(req.getDocumentDate() != null ? req.getDocumentDate() : postingDate)
                .periodId(period.getId())
                .description(req.getDescription())
                .sourceType(req.getSourceType() != null ? req.getSourceType() : PostingSource.MANUAL)
                .sourceId(req.getSourceId())
                .idempotencyKey(req.getIdempotencyKey())
                .status(targetStatus)
                .totalDebit(totalDr)
                .totalCredit(totalCr)
                .postedAt(targetStatus == JournalStatus.POSTED ? LocalDateTime.now() : null)
                .build();
        entry.setIsDeleted(false);
        entryRepo.save(entry);

        int lineNo = 1;
        List<JournalEntryLine> savedLines = new ArrayList<>();
        for (JournalLineRequest l : req.getLines()) {
            Account acc = accCache.get(l.getAccountCode());
            JournalEntryLine line = JournalEntryLine.builder()
                    .journalEntryId(entry.getId())
                    .lineNo(lineNo++)
                    .accountId(acc.getId())
                    .accountCode(acc.getCode())
                    .debit(l.getDebit() != null ? l.getDebit() : BigDecimal.ZERO)
                    .credit(l.getCredit() != null ? l.getCredit() : BigDecimal.ZERO)
                    .description(l.getDescription())
                    .departmentId(l.getDepartmentId())
                    .partnerType(l.getPartnerType())
                    .partnerId(l.getPartnerId())
                    .partnerName(l.getPartnerName())
                    .projectId(l.getProjectId())
                    .build();
            line.setIsDeleted(false);
            savedLines.add(lineRepo.save(line));
        }
        entry.setLines(savedLines);
        return toResponse(entry, savedLines, accCache);
    }

    private void assertPeriodStatus(FiscalPeriod p) {
        if (p.getStatus() != PeriodStatus.OPEN) {
            throw new AppException(AccountingErrorCode.PERIOD_CLOSED, p.getMonth() + "/" + p.getYear());
        }
    }

    /**
     * Sinh số chứng từ dạng JV-YYYY-MM-nnnn. Đơn giản: dùng UUID suffix để tránh conflict.
     * Có thể thay bằng CodeSequence nếu cần strict sequential.
     */
    private String nextCode(LocalDate date) {
        String prefix = String.format("JV-%04d-%02d-", date.getYear(), date.getMonthValue());
        String rand = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + rand;
    }

    private JournalEntryResponse withLines(JournalEntry e) {
        List<JournalEntryLine> lines = lineRepo.findByJournalEntryIdOrderByLineNoAsc(e.getId());
        Map<String, Account> accCache = new HashMap<>();
        for (JournalEntryLine l : lines) {
            accCache.computeIfAbsent(l.getAccountCode(),
                    c -> accountRepo.findByCodeAndIsDeletedFalse(c).orElse(null));
        }
        return toResponse(e, lines, accCache);
    }

    private JournalEntryResponse toResponse(JournalEntry e, List<JournalEntryLine> lines,
                                            Map<String, Account> accCache) {
        return JournalEntryResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .postingDate(e.getPostingDate())
                .documentDate(e.getDocumentDate())
                .periodId(e.getPeriodId())
                .description(e.getDescription())
                .sourceType(e.getSourceType())
                .sourceId(e.getSourceId())
                .idempotencyKey(e.getIdempotencyKey())
                .status(e.getStatus())
                .totalDebit(e.getTotalDebit())
                .totalCredit(e.getTotalCredit())
                .postedAt(e.getPostedAt())
                .postedBy(e.getPostedBy())
                .reversalOfId(e.getReversalOfId())
                .lines(lines.stream().map(l -> {
                    Account a = accCache.get(l.getAccountCode());
                    return JournalLineResponse.builder()
                            .id(l.getId())
                            .journalEntryId(l.getJournalEntryId())
                            .lineNo(l.getLineNo())
                            .accountId(l.getAccountId())
                            .accountCode(l.getAccountCode())
                            .accountName(a != null ? a.getName() : null)
                            .debit(l.getDebit())
                            .credit(l.getCredit())
                            .description(l.getDescription())
                            .departmentId(l.getDepartmentId())
                            .partnerType(l.getPartnerType())
                            .partnerId(l.getPartnerId())
                            .partnerName(l.getPartnerName())
                            .projectId(l.getProjectId())
                            .build();
                }).toList())
                .build();
    }
}
