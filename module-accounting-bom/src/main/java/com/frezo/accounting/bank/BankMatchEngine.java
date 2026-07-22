package com.frezo.accounting.bank;

import com.frezo.accounting.dto.response.MatchSuggestionDto;
import com.frezo.accounting.entity.BankStatementLine;
import com.frezo.accounting.entity.JournalEntry;
import com.frezo.accounting.entity.JournalEntryLine;
import com.frezo.accounting.repository.JournalEntryLineRepository;
import com.frezo.accounting.repository.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Auto-match exact + fuzzy suggestions (amount50 + date30 + desc20).
 */
@Component
@RequiredArgsConstructor
public class BankMatchEngine {

    private final JournalEntryLineRepository journalEntryLineRepository;
    private final JournalEntryRepository journalEntryRepository;

    public record AutoMatchResult(boolean matched, String journalLineId) {}

    public AutoMatchResult tryAutoMatch(String accountId, BankStatementLine line) {
        BigDecimal amount = amountOf(line);
        List<JournalEntryLine> candidates = journalEntryLineRepository
                .findMatchCandidates(accountId, line.getTxnDate(), amount);
        if (candidates.size() == 1) {
            return new AutoMatchResult(true, candidates.get(0).getId());
        }
        return new AutoMatchResult(false, null);
    }

    public List<MatchSuggestionDto> suggestions(String accountId, BankStatementLine line) {
        return suggestions(accountId, line, "exact");
    }

    public List<MatchSuggestionDto> suggestions(String accountId, BankStatementLine line, String mode) {
        boolean fuzzy = mode != null && "fuzzy".equalsIgnoreCase(mode);
        BigDecimal amount = amountOf(line);
        List<JournalEntryLine> candidates;
        if (fuzzy) {
            LocalDate d = line.getTxnDate() != null ? line.getTxnDate() : LocalDate.now();
            candidates = journalEntryLineRepository.findFuzzyCandidates(
                    accountId, d.minusDays(7), d.plusDays(7));
        } else {
            candidates = journalEntryLineRepository
                    .findMatchCandidates(accountId, line.getTxnDate(), amount);
        }

        List<MatchSuggestionDto> out = new ArrayList<>();
        for (JournalEntryLine jel : candidates) {
            JournalEntry je = journalEntryRepository.findById(jel.getJournalEntryId()).orElse(null);
            BigDecimal a = jel.getDebit() != null && jel.getDebit().signum() > 0
                    ? jel.getDebit() : (jel.getCredit() != null ? jel.getCredit() : BigDecimal.ZERO);
            LocalDate postingDate = je != null ? je.getPostingDate() : null;
            int score = fuzzy
                    ? scoreFuzzy(amount, line.getTxnDate(), line.getDescription(), a, postingDate, jel.getDescription())
                    : 100;
            if (fuzzy && score < 30) continue;
            out.add(MatchSuggestionDto.builder()
                    .journalEntryLineId(jel.getId())
                    .journalEntryCode(je != null ? je.getCode() : null)
                    .txnDate(postingDate != null ? postingDate.toString() : null)
                    .description(jel.getDescription())
                    .amount(a.doubleValue())
                    .score(score)
                    .reason(fuzzy
                            ? String.format("fuzzy amount+date+desc = %d", score)
                            : "Khớp exact amount + date")
                    .build());
        }
        out.sort(Comparator.comparing(MatchSuggestionDto::getScore,
                Comparator.nullsLast(Comparator.reverseOrder())));
        if (out.size() > 20) {
            return new ArrayList<>(out.subList(0, 20));
        }
        return out;
    }

    /** amount50 + date30 + desc20 */
    public static int scoreFuzzy(BigDecimal bankAmt, LocalDate bankDate, String bankDesc,
                                 BigDecimal jeAmt, LocalDate jeDate, String jeDesc) {
        int amountScore = 0;
        if (bankAmt != null && jeAmt != null) {
            if (bankAmt.compareTo(jeAmt) == 0) {
                amountScore = 50;
            } else if (bankAmt.signum() != 0) {
                BigDecimal diff = bankAmt.subtract(jeAmt).abs();
                BigDecimal pct = diff.divide(bankAmt.abs(), 4, RoundingMode.HALF_UP);
                if (pct.compareTo(new BigDecimal("0.01")) <= 0) amountScore = 40;
                else if (pct.compareTo(new BigDecimal("0.05")) <= 0) amountScore = 25;
                else if (pct.compareTo(new BigDecimal("0.10")) <= 0) amountScore = 10;
            }
        }

        int dateScore = 0;
        if (bankDate != null && jeDate != null) {
            long days = Math.abs(ChronoUnit.DAYS.between(bankDate, jeDate));
            if (days == 0) dateScore = 30;
            else if (days == 1) dateScore = 20;
            else if (days <= 3) dateScore = 10;
            else if (days <= 7) dateScore = 5;
        }

        int descScore = descOverlap(bankDesc, jeDesc);
        return amountScore + dateScore + descScore;
    }

    private static int descOverlap(String a, String b) {
        if (a == null || b == null || a.isBlank() || b.isBlank()) return 0;
        String[] ta = a.toLowerCase(Locale.ROOT).split("\\W+");
        String bl = b.toLowerCase(Locale.ROOT);
        int hits = 0;
        int meaningful = 0;
        for (String t : ta) {
            if (t.length() < 3) continue;
            meaningful++;
            if (bl.contains(t)) hits++;
        }
        if (meaningful == 0) return 0;
        return Math.min(20, (hits * 20) / meaningful);
    }

    private static BigDecimal amountOf(BankStatementLine line) {
        BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;
        BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
        return credit.signum() > 0 ? credit : debit;
    }
}
