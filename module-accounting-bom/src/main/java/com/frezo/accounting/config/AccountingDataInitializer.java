package com.frezo.accounting.config;

import com.frezo.accounting.common.AccountingStandard;
import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.dto.request.JournalEntryRequest;
import com.frezo.accounting.dto.request.JournalLineRequest;
import com.frezo.accounting.entity.Account;
import com.frezo.accounting.entity.BankStatement;
import com.frezo.accounting.entity.BankStatementLine;
import com.frezo.accounting.repository.AccountRepository;
import com.frezo.accounting.repository.BankStatementLineRepository;
import com.frezo.accounting.repository.BankStatementRepository;
import com.frezo.accounting.repository.JournalEntryRepository;
import com.frezo.accounting.service.AccountService;
import com.frezo.accounting.service.AccountingSettingService;
import com.frezo.accounting.service.FiscalPeriodService;
import com.frezo.accounting.service.JournalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Seed dữ liệu mẫu Accounting (idempotent) — settings TT133, COA, fiscal year,
 * journal DRAFT+POSTED, bank statement — để FE/QA chạy được các màn kế toán.
 * <p>
 * Bật khi profile {@code local} (default) hoặc {@code seed}.
 * Không chạy trên {@code prod}.
 */
@Slf4j
@Component
@Order(90)
@Profile({"local", "seed"})
@RequiredArgsConstructor
public class AccountingDataInitializer implements ApplicationRunner {

    /** Marker fileName — dùng để skip khi đã seed bank statement. */
    public static final String SEED_BANK_FILE = "seed-bank-statement.csv";

    public static final String KEY_OPENING = "SEED-ACC-OPENING";
    public static final String KEY_BANK_DEPOSIT = "SEED-ACC-BANK-DEPOSIT";
    public static final String KEY_REVENUE = "SEED-ACC-REVENUE";
    public static final String KEY_EXPENSE_DRAFT = "SEED-ACC-EXPENSE-DRAFT";

    private final AccountingSettingService settingService;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final FiscalPeriodService fiscalPeriodService;
    private final JournalService journalService;
    private final JournalEntryRepository journalEntryRepository;
    private final BankStatementRepository bankStatementRepository;
    private final BankStatementLineRepository bankStatementLineRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            transactionTemplate.executeWithoutResult(status -> seedAll());
            log.info("[acc-seed] Accounting demo data ready");
        } catch (Exception ex) {
            log.error("[acc-seed] failed — app continues without accounting sample data", ex);
        }
    }

    private void seedAll() {
        settingService.getOrCreateDefault();
        int coaCreated = accountService.seedChartOfAccounts(AccountingStandard.TT133);
        log.info("[acc-seed] settings TT133 ok; COA created={}", coaCreated);

        int year = LocalDate.now().getYear();
        fiscalPeriodService.ensureYear(year);
        log.info("[acc-seed] fiscal year {} + 12 periods OPEN", year);

        LocalDate openingDate = LocalDate.of(year, 1, 5);
        LocalDate depositDate = dayInCurrentMonth(10);
        LocalDate revenueDate = dayInCurrentMonth(12);
        LocalDate expenseDate = dayInCurrentMonth(15);

        seedPosted(KEY_OPENING, openingDate,
                "SEED — Góp vốn bằng tiền mặt",
                line("1111", bd("100000000"), ZERO, "Tiền mặt nhận góp vốn"),
                line("411", ZERO, bd("100000000"), "Vốn chủ sở hữu"));

        seedPosted(KEY_BANK_DEPOSIT, depositDate,
                "SEED — Nộp tiền mặt vào ngân hàng",
                line("1121", bd("50000000"), ZERO, "Nộp tiền vào TK NH"),
                line("1111", ZERO, bd("50000000"), "Xuất quỹ tiền mặt"));

        seedPosted(KEY_REVENUE, revenueDate,
                "SEED — Thu tiền dịch vụ qua ngân hàng",
                line("1121", bd("10000000"), ZERO, "Thu dịch vụ qua NH"),
                line("5113", ZERO, bd("10000000"), "Doanh thu cung cấp dịch vụ"));

        seedDraft(KEY_EXPENSE_DRAFT, expenseDate,
                "SEED — Chi phí quản lý (nháp)",
                line("6422", bd("2000000"), ZERO, "Chi văn phòng phẩm"),
                line("1111", ZERO, bd("2000000"), "Chi tiền mặt"));

        seedBankStatement(depositDate, revenueDate);
    }

    private void seedPosted(String idempotencyKey, LocalDate date, String description,
                            JournalLineRequest... lines) {
        if (journalEntryRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.debug("[acc-seed] journal {} already exists — skip", idempotencyKey);
            return;
        }
        JournalEntryRequest req = buildRequest(idempotencyKey, date, description, lines);
        journalService.createAndPost(req);
        log.info("[acc-seed] POSTED journal {}", idempotencyKey);
    }

    private void seedDraft(String idempotencyKey, LocalDate date, String description,
                           JournalLineRequest... lines) {
        if (journalEntryRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.debug("[acc-seed] journal {} already exists — skip", idempotencyKey);
            return;
        }
        JournalEntryRequest req = buildRequest(idempotencyKey, date, description, lines);
        journalService.createDraft(req);
        log.info("[acc-seed] DRAFT journal {}", idempotencyKey);
    }

    private JournalEntryRequest buildRequest(String key, LocalDate date, String description,
                                             JournalLineRequest... lines) {
        JournalEntryRequest req = new JournalEntryRequest();
        req.setPostingDate(date);
        req.setDocumentDate(date);
        req.setDescription(description);
        req.setSourceType(PostingSource.MANUAL);
        req.setSourceId("SEED-ACC");
        req.setIdempotencyKey(key);
        req.setLines(List.of(lines));
        return req;
    }

    private void seedBankStatement(LocalDate depositDate, LocalDate revenueDate) {
        boolean exists = bankStatementRepository.findByIsDeletedFalseOrderByImportedAtDesc().stream()
                .anyMatch(s -> SEED_BANK_FILE.equals(s.getFileName()));
        if (exists) {
            log.debug("[acc-seed] bank statement {} already exists — skip", SEED_BANK_FILE);
            return;
        }

        Account bank = accountRepository.findByCodeAndIsDeletedFalse("1121")
                .orElse(null);
        if (bank == null) {
            log.warn("[acc-seed] TK 1121 missing — skip bank statement");
            return;
        }

        BankStatement stmt = BankStatement.builder()
                .accountId(bank.getId())
                .accountCode(bank.getCode())
                .accountName(bank.getName())
                .fileName(SEED_BANK_FILE)
                .importedAt(LocalDateTime.now())
                .importedLines(3)
                .matchedCount(0)
                .unmatchedCount(3)
                .status("OPEN")
                .build();
        stmt.setId(UUID.randomUUID().toString());
        stmt.setIsDeleted(false);
        stmt = bankStatementRepository.save(stmt);

        // Credit = tiền vào NH (khớp journal Dr 1121 cùng số tiền + ngày)
        saveBankLine(stmt.getId(), depositDate, "Nộp tiền mặt vào NH", "SEED-REF-DEP",
                ZERO, bd("50000000"), bd("50000000"));
        saveBankLine(stmt.getId(), revenueDate, "Thu dịch vụ khách hàng", "SEED-REF-REV",
                ZERO, bd("10000000"), bd("60000000"));
        // Unmatched fee — để FE/QA test unmatched list
        saveBankLine(stmt.getId(), revenueDate.plusDays(1), "Phí quản lý tài khoản", "SEED-REF-FEE",
                bd("55000"), ZERO, bd("59945000"));

        stmt.setMatchedCount(0);
        stmt.setUnmatchedCount(3);
        bankStatementRepository.save(stmt);
        log.info("[acc-seed] bank statement {} on 1121 (3 lines)", SEED_BANK_FILE);
    }

    private void saveBankLine(String statementId, LocalDate txnDate, String desc, String ref,
                              BigDecimal debit, BigDecimal credit, BigDecimal balance) {
        BankStatementLine line = BankStatementLine.builder()
                .statementId(statementId)
                .txnDate(txnDate)
                .description(desc)
                .refCode(ref)
                .debit(debit)
                .credit(credit)
                .balance(balance)
                .matchStatus("UNMATCHED")
                .build();
        line.setId(UUID.randomUUID().toString());
        line.setIsDeleted(false);
        bankStatementLineRepository.save(line);
    }

    private static JournalLineRequest line(String code, BigDecimal debit, BigDecimal credit, String desc) {
        JournalLineRequest l = new JournalLineRequest();
        l.setAccountCode(code);
        l.setDebit(debit);
        l.setCredit(credit);
        l.setDescription(desc);
        return l;
    }

    private static LocalDate dayInCurrentMonth(int day) {
        YearMonth ym = YearMonth.now();
        return ym.atDay(Math.min(day, ym.lengthOfMonth()));
    }

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
