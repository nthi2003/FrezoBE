package com.frezo.accounting.service.impl;

import com.frezo.accounting.common.AccountType;
import com.frezo.accounting.common.AccountingErrorCode;
import com.frezo.accounting.dto.response.GLLineResponse;
import com.frezo.accounting.dto.response.GLResponse;
import com.frezo.accounting.dto.response.TrialBalanceRowResponse;
import com.frezo.accounting.entity.Account;
import com.frezo.accounting.entity.JournalEntry;
import com.frezo.accounting.entity.JournalEntryLine;
import com.frezo.accounting.repository.AccountRepository;
import com.frezo.accounting.repository.JournalEntryLineRepository;
import com.frezo.accounting.repository.JournalEntryRepository;
import com.frezo.accounting.service.GLService;
import com.frezo.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GLServiceImpl implements GLService {

    private final AccountRepository accountRepo;
    private final JournalEntryRepository entryRepo;
    private final JournalEntryLineRepository lineRepo;

    @Override
    @Transactional(readOnly = true)
    public GLResponse getLedger(String accountCode, LocalDate from, LocalDate to) {
        Account acc = accountRepo.findByCode(accountCode)
                .orElseThrow(() -> new AppException(AccountingErrorCode.ACCOUNT_NOT_FOUND, accountCode));

        // Số dư đầu kỳ = tổng debit - tổng credit tính đến ngay trước `from`.
        LocalDate dayBefore = from.minusDays(1);
        BigDecimal debitBefore = safe(lineRepo.sumDebitToDate(acc.getId(), dayBefore));
        BigDecimal creditBefore = safe(lineRepo.sumCreditToDate(acc.getId(), dayBefore));
        BigDecimal opening = debitBefore.subtract(creditBefore); // dương = dư Nợ, âm = dư Có

        BigDecimal openingBalanceInit = acc.getOpeningBalance() != null
                ? acc.getOpeningBalance() : BigDecimal.ZERO;
        // Nếu TK có openingBalance được set (số dư 01/01/xxxx) — cộng thêm cho các TK không có phát sinh trước đó
        // (giả định openingBalance là dư Nợ khi > 0, dư Có khi < 0 — quy ước)
        opening = opening.add(openingBalanceInit);

        BigDecimal openingDebit = opening.signum() > 0 ? opening : BigDecimal.ZERO;
        BigDecimal openingCredit = opening.signum() < 0 ? opening.negate() : BigDecimal.ZERO;

        // Lines trong kỳ
        List<JournalEntryLine> lines = lineRepo.findGLLines(acc.getId(), from, to);
        Map<String, JournalEntry> entriesById = new HashMap<>();
        for (JournalEntryLine l : lines) {
            entriesById.computeIfAbsent(l.getJournalEntryId(),
                    id -> entryRepo.findById(id).orElse(null));
        }

        List<GLLineResponse> out = new ArrayList<>();
        BigDecimal runningDr = openingDebit;
        BigDecimal runningCr = openingCredit;
        BigDecimal periodDr = BigDecimal.ZERO;
        BigDecimal periodCr = BigDecimal.ZERO;
        for (JournalEntryLine l : lines) {
            JournalEntry je = entriesById.get(l.getJournalEntryId());
            runningDr = runningDr.add(safe(l.getDebit()));
            runningCr = runningCr.add(safe(l.getCredit()));
            periodDr = periodDr.add(safe(l.getDebit()));
            periodCr = periodCr.add(safe(l.getCredit()));
            out.add(GLLineResponse.builder()
                    .journalEntryId(je != null ? je.getId() : l.getJournalEntryId())
                    .journalCode(je != null ? je.getCode() : null)
                    .postingDate(je != null ? je.getPostingDate() : null)
                    .description(l.getDescription() != null ? l.getDescription()
                            : je != null ? je.getDescription() : null)
                    .debit(safe(l.getDebit()))
                    .credit(safe(l.getCredit()))
                    .runningDebit(isDebitNature(acc.getType()) ? runningDr.subtract(runningCr).max(BigDecimal.ZERO) : null)
                    .runningCredit(!isDebitNature(acc.getType()) ? runningCr.subtract(runningDr).max(BigDecimal.ZERO) : null)
                    .partnerName(l.getPartnerName())
                    .departmentId(l.getDepartmentId())
                    .build());
        }

        BigDecimal closing = openingDebit.subtract(openingCredit).add(periodDr).subtract(periodCr);

        return GLResponse.builder()
                .accountId(acc.getId())
                .accountCode(acc.getCode())
                .accountName(acc.getName())
                .from(from)
                .to(to)
                .openingDebit(openingDebit)
                .openingCredit(openingCredit)
                .periodDebit(periodDr)
                .periodCredit(periodCr)
                .closingDebit(closing.signum() > 0 ? closing : BigDecimal.ZERO)
                .closingCredit(closing.signum() < 0 ? closing.negate() : BigDecimal.ZERO)
                .lines(out)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TrialBalanceRowResponse> trialBalance(LocalDate from, LocalDate to) {
        Map<String, Account> byCode = new HashMap<>();
        for (Account a : accountRepo.findByIsDeletedFalseOrderByCodeAsc()) {
            byCode.put(a.getCode(), a);
        }

        List<Object[]> rows = lineRepo.aggregateByAccount(from, to);
        List<TrialBalanceRowResponse> out = new ArrayList<>();
        LocalDate dayBefore = from.minusDays(1);
        for (Object[] r : rows) {
            String accId = (String) r[0];
            String code = (String) r[1];
            BigDecimal dr = safe((BigDecimal) r[2]);
            BigDecimal cr = safe((BigDecimal) r[3]);

            BigDecimal debitBefore = safe(lineRepo.sumDebitToDate(accId, dayBefore));
            BigDecimal creditBefore = safe(lineRepo.sumCreditToDate(accId, dayBefore));
            BigDecimal opening = debitBefore.subtract(creditBefore);
            BigDecimal openingDr = opening.signum() > 0 ? opening : BigDecimal.ZERO;
            BigDecimal openingCr = opening.signum() < 0 ? opening.negate() : BigDecimal.ZERO;

            BigDecimal closingRaw = opening.add(dr).subtract(cr);
            BigDecimal closingDr = closingRaw.signum() > 0 ? closingRaw : BigDecimal.ZERO;
            BigDecimal closingCr = closingRaw.signum() < 0 ? closingRaw.negate() : BigDecimal.ZERO;

            Account acc = byCode.get(code);
            out.add(TrialBalanceRowResponse.builder()
                    .accountId(accId)
                    .accountCode(code)
                    .accountName(acc != null ? acc.getName() : code)
                    .openingDebit(openingDr)
                    .openingCredit(openingCr)
                    .periodDebit(dr)
                    .periodCredit(cr)
                    .closingDebit(closingDr)
                    .closingCredit(closingCr)
                    .build());
        }
        return out;
    }

    private static boolean isDebitNature(AccountType t) {
        return t == AccountType.ASSET || t == AccountType.EXPENSE;
    }

    private static BigDecimal safe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
