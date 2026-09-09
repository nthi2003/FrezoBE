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

    /**
     * Liệt kê danh sách sao kê ngân hàng (chưa xóa), mới nhất trước.
     */
    @Override
    public List<BankStatementDto> list() {
        return statementRepository.findByIsDeletedFalseOrderByImportedAtDesc().stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Nhập file CSV sao kê cho tài khoản ngân hàng (mã TK bắt đầu 112), tạo bản sao kê mới
     * và tự động đối chiếu từng dòng nghiệp vụ với chứng từ kế toán.
     */
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

    /**
     * Liệt kê các dòng sao kê của một bản sao kê, lọc theo trạng thái đối chiếu (matched/unmatched/all).
     */
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

    /**
     * Đưa ra các gợi ý chứng từ khớp cho một dòng sao kê chưa đối chiếu (theo chế độ exact/fuzzy).
     */
    @Override
    public List<MatchSuggestionDto> suggestions(String statementId, String lineId, String mode) {
        BankStatement stmt = findStmt(statementId);
        assertNotLocked(stmt);
        BankStatementLine line = findLine(lineId);
        if ("MATCHED".equals(line.getMatchStatus())) return List.of();
        return matchEngine.suggestions(stmt.getAccountId(), line, mode != null ? mode : "exact");
    }

    /**
     * Đối chiếu thủ công một dòng sao kê với một dòng chứng từ kế toán và cập nhật lại số liệu đã khớp.
     */
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

    /**
     * Hủy đối chiếu một dòng sao kê đã khớp và cập nhật lại số liệu khớp/chưa khớp.
     */
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

    /**
     * Khóa bản sao kê để ngăn các thao tác đối chiếu/hủy đối chiếu về sau.
     */
    @Override
    @Transactional
    public BankStatementDto lock(String id) {
        BankStatement stmt = findStmt(id);
        stmt.setStatus(STATUS_LOCKED);
        return toDto(statementRepository.save(stmt));
    }

    /**
     * Mở lại bản sao kê đã khóa để có thể tiếp tục đối chiếu.
     */
    @Override
    @Transactional
    public BankStatementDto reopen(String id) {
        BankStatement stmt = findStmt(id);
        stmt.setStatus(STATUS_OPEN);
        return toDto(statementRepository.save(stmt));
    }

    /**
     * Ném lỗi nếu bản sao kê đang ở trạng thái LOCKED (không cho match/unmatch).
     */
    private void assertNotLocked(BankStatement stmt) {
        if (STATUS_LOCKED.equals(stmt.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT,
                    "Statement đã LOCKED — không thể match/unmatch");
        }
    }

    /**
     * Tính lại và lưu số dòng đã khớp/chưa khớp của bản sao kê sau khi match/unmatch.
     */
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

    /**
     * Kiểm tra dòng sao kê có thỏa bộ lọc trạng thái đối chiếu được yêu cầu không.
     */
    private boolean filterStatus(BankStatementLine l, String status) {
        if (status == null || status.isBlank() || "all".equalsIgnoreCase(status)) return true;
        if ("matched".equalsIgnoreCase(status)) return "MATCHED".equals(l.getMatchStatus());
        if ("unmatched".equalsIgnoreCase(status)) return "UNMATCHED".equals(l.getMatchStatus());
        return true;
    }

    /**
     * Tìm bản sao kê theo id, chỉ trả về bản chưa xóa; ném lỗi nếu không tồn tại.
     */
    private BankStatement findStmt(String id) {
        return statementRepository.findById(id)
                .filter(s -> Boolean.FALSE.equals(s.getIsDeleted()) || s.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Statement không tồn tại"));
    }

    /**
     * Tìm dòng sao kê theo id, chỉ trả về dòng chưa xóa; ném lỗi nếu không tồn tại.
     */
    private BankStatementLine findLine(String id) {
        return lineRepository.findById(id)
                .filter(l -> Boolean.FALSE.equals(l.getIsDeleted()) || l.getIsDeleted() == null)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Line không tồn tại"));
    }

    /**
     * Chuyển entity BankStatement thành DTO phản hồi.
     */
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

    /**
     * Chuyển entity BankStatementLine thành DTO phản hồi.
     */
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
