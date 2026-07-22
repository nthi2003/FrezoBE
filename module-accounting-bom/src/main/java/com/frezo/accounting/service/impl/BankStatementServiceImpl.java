package com.frezo.accounting.service.impl;

import com.frezo.accounting.bank.BankCsvParser;
import com.frezo.accounting.bank.BankMatchEngine;
import com.frezo.accounting.dto.response.BankStatementDto;
import com.frezo.accounting.dto.response.BankStatementLineDto;
import com.frezo.accounting.dto.response.MatchSuggestionDto;
import com.frezo.accounting.entity.Account;
import com.frezo.accounting.entity.BankStatement;
import com.frezo.accounting.entity.BankStatementLine;
import com.frezo.accounting.repository.AccountRepository;
import com.frezo.accounting.repository.BankStatementLineRepository;
import com.frezo.accounting.repository.BankStatementRepository;
import com.frezo.accounting.service.BankStatementService;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.response.FePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankStatementServiceImpl implements BankStatementService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_LOCKED = "LOCKED";

    private final BankStatementRepository statementRepository;
    private final BankStatementLineRepository lineRepository;
    private final AccountRepository accountRepository;
    private final BankMatchEngine matchEngine;

    @Override
    public List<BankStatementDto> list() {
        return statementRepository.findByIsDeletedFalseOrderByImportedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public BankStatementDto importCsv(String accountId, MultipartFile file) {
        Account account = accountRepository.findById(accountId)
                .filter(a -> Boolean.FALSE.equals(a.getIsDeleted()) || a.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Tài khoản không tồn tại"));
        if (account.getCode() == null || !account.getCode().startsWith("112")) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST,
                    "bankAccountId phải là TK bắt đầu bằng 112 (tiền gửi ngân hàng)");
        }

        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Không đọc được file CSV");
        }
        List<BankCsvParser.ParsedLine> parsed = BankCsvParser.parse(text);

        BankStatement stmt = BankStatement.builder()
                .accountId(account.getId())
                .accountCode(account.getCode())
                .accountName(account.getName())
                .fileName(file.getOriginalFilename())
                .importedAt(LocalDateTime.now())
                .importedLines(parsed.size())
                .matchedCount(0)
                .unmatchedCount(parsed.size())
                .status(STATUS_OPEN)
                .build();
        stmt.setId(UUID.randomUUID().toString());
        stmt = statementRepository.save(stmt);

        int matched = 0;
        for (BankCsvParser.ParsedLine p : parsed) {
            BankStatementLine line = BankStatementLine.builder()
                    .statementId(stmt.getId())
                    .txnDate(p.txnDate())
                    .description(p.description())
                    .refCode(p.refCode())
                    .debit(p.debit() != null ? p.debit() : BigDecimal.ZERO)
                    .credit(p.credit() != null ? p.credit() : BigDecimal.ZERO)
                    .balance(p.balance())
                    .matchStatus("UNMATCHED")
                    .build();
            line.setId(UUID.randomUUID().toString());

            BankMatchEngine.AutoMatchResult auto = matchEngine.tryAutoMatch(account.getId(), line);
            if (auto.matched()) {
                line.setMatchStatus("MATCHED");
                line.setMatchedJournalLineId(auto.journalLineId());
                matched++;
            }
            lineRepository.save(line);
        }

        stmt.setMatchedCount(matched);
        stmt.setUnmatchedCount(parsed.size() - matched);
        return toDto(statementRepository.save(stmt));
    }

    @Override
    public FePage<BankStatementLineDto> listLines(String statementId, String status) {
        List<BankStatementLineDto> all = lineRepository
                .findByStatementIdAndIsDeletedFalseOrderByTxnDateAsc(statementId)
                .stream()
                .filter(l -> filterStatus(l, status))
                .map(this::toLineDto)
                .toList();
        return FePage.all(all);
    }

    @Override
    public List<MatchSuggestionDto> suggestions(String statementId, String lineId, String mode) {
        BankStatement stmt = findStmt(statementId);
        assertNotLocked(stmt);
        BankStatementLine line = findLine(lineId);
        if ("MATCHED".equals(line.getMatchStatus())) return List.of();
        return matchEngine.suggestions(stmt.getAccountId(), line, mode != null ? mode : "exact");
    }

    @Override
    @Transactional
    public BankStatementLineDto match(String lineId, String journalEntryLineId) {
        BankStatementLine line = findLine(lineId);
        BankStatement stmt = findStmt(line.getStatementId());
        assertNotLocked(stmt);
        line.setMatchStatus("MATCHED");
        line.setMatchedJournalLineId(journalEntryLineId);
        lineRepository.save(line);
        refreshCounts(line.getStatementId());
        return toLineDto(line);
    }

    @Override
    @Transactional
    public BankStatementLineDto unmatch(String lineId) {
        BankStatementLine line = findLine(lineId);
        BankStatement stmt = findStmt(line.getStatementId());
        assertNotLocked(stmt);
        line.setMatchStatus("UNMATCHED");
        line.setMatchedJournalLineId(null);
        lineRepository.save(line);
        refreshCounts(line.getStatementId());
        return toLineDto(line);
    }

    @Override
    @Transactional
    public BankStatementDto lock(String id) {
        BankStatement stmt = findStmt(id);
        stmt.setStatus(STATUS_LOCKED);
        return toDto(statementRepository.save(stmt));
    }

    @Override
    @Transactional
    public BankStatementDto reopen(String id) {
        BankStatement stmt = findStmt(id);
        stmt.setStatus(STATUS_OPEN);
        return toDto(statementRepository.save(stmt));
    }

    private void assertNotLocked(BankStatement stmt) {
        if (STATUS_LOCKED.equals(stmt.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT,
                    "Statement đã LOCKED — không thể match/unmatch");
        }
    }

    private void refreshCounts(String statementId) {
        List<BankStatementLine> lines = lineRepository
                .findByStatementIdAndIsDeletedFalseOrderByTxnDateAsc(statementId);
        int matched = (int) lines.stream().filter(l -> "MATCHED".equals(l.getMatchStatus())).count();
        statementRepository.findById(statementId).ifPresent(s -> {
            s.setMatchedCount(matched);
            s.setUnmatchedCount(lines.size() - matched);
            statementRepository.save(s);
        });
    }

    private boolean filterStatus(BankStatementLine l, String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) return true;
        if ("matched".equalsIgnoreCase(status)) return "MATCHED".equals(l.getMatchStatus());
        if ("unmatched".equalsIgnoreCase(status)) return "UNMATCHED".equals(l.getMatchStatus());
        return true;
    }

    private BankStatement findStmt(String id) {
        return statementRepository.findById(id)
                .filter(s -> Boolean.FALSE.equals(s.getIsDeleted()) || s.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Statement không tồn tại"));
    }

    private BankStatementLine findLine(String id) {
        return lineRepository.findById(id)
                .filter(l -> Boolean.FALSE.equals(l.getIsDeleted()) || l.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Line không tồn tại"));
    }

    private BankStatementDto toDto(BankStatement s) {
        return BankStatementDto.builder()
                .id(s.getId())
                .accountId(s.getAccountId())
                .accountCode(s.getAccountCode())
                .accountName(s.getAccountName())
                .importedAt(s.getImportedAt() != null ? s.getImportedAt().format(ISO) : null)
                .importedLines(s.getImportedLines())
                .matchedCount(s.getMatchedCount())
                .unmatchedCount(s.getUnmatchedCount())
                .fileName(s.getFileName())
                .status(s.getStatus() != null ? s.getStatus() : STATUS_OPEN)
                .build();
    }

    private BankStatementLineDto toLineDto(BankStatementLine l) {
        return BankStatementLineDto.builder()
                .id(l.getId())
                .statementId(l.getStatementId())
                .txnDate(l.getTxnDate() != null ? l.getTxnDate().toString() : null)
                .description(l.getDescription())
                .refCode(l.getRefCode())
                .debit(l.getDebit() != null ? l.getDebit().doubleValue() : 0.0)
                .credit(l.getCredit() != null ? l.getCredit().doubleValue() : 0.0)
                .balance(l.getBalance() != null ? l.getBalance().doubleValue() : null)
                .matchStatus(l.getMatchStatus())
                .matchedJournalLineId(l.getMatchedJournalLineId())
                .build();
    }
}
