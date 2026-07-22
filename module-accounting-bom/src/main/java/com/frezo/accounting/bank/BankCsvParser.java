package com.frezo.accounting.bank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stateless CSV parser — GENERIC_CSV (date, description, ref, debit, credit, balance).
 */
public final class BankCsvParser {

    private BankCsvParser() {}

    public record ParsedLine(
            LocalDate txnDate,
            String description,
            String refCode,
            BigDecimal debit,
            BigDecimal credit,
            BigDecimal balance
    ) {}

    public static List<ParsedLine> parse(String text) {
        if (text == null || text.isBlank()) return List.of();
        String cleaned = text.replace("\uFEFF", "");
        String[] rawLines = cleaned.split("\\r?\\n");
        List<String> lines = new ArrayList<>();
        for (String l : rawLines) {
            if (l != null && !l.trim().isEmpty()) lines.add(l.trim());
        }
        if (lines.size() < 2) return List.of();

        List<String> header = splitCsvRow(lines.get(0)).stream()
                .map(h -> h.toLowerCase(Locale.ROOT).trim())
                .toList();
        int idxDate = findCol(header, List.of("date", "txndate", "ngày", "ngay", "txn_date"));
        int idxDesc = findCol(header, List.of("description", "desc", "nội dung", "noi dung", "memo"));
        int idxRef = findCol(header, List.of("ref", "refcode", "reference", "mã gd", "ma gd"));
        int idxDebit = findCol(header, List.of("debit", "nợ", "no", "withdrawal", "chi"));
        int idxCredit = findCol(header, List.of("credit", "có", "co", "deposit", "thu"));
        int idxBalance = findCol(header, List.of("balance", "số dư", "so du"));

        List<ParsedLine> out = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            List<String> cols = splitCsvRow(lines.get(i));
            LocalDate date = parseDate(col(cols, idxDate));
            String desc = col(cols, idxDesc);
            BigDecimal debit = parseNum(col(cols, idxDebit));
            BigDecimal credit = parseNum(col(cols, idxCredit));
            if (date == null && debit.signum() == 0 && credit.signum() == 0 && (desc == null || desc.isBlank())) {
                continue;
            }
            if (date == null) continue;
            out.add(new ParsedLine(
                    date,
                    desc != null ? desc : "",
                    idxRef >= 0 ? col(cols, idxRef) : null,
                    debit,
                    credit,
                    idxBalance >= 0 ? parseNum(col(cols, idxBalance)) : null
            ));
        }
        return out;
    }

    private static String col(List<String> cols, int idx) {
        if (idx < 0 || idx >= cols.size()) return null;
        return cols.get(idx);
    }

    private static int findCol(List<String> header, List<String> aliases) {
        for (String a : aliases) {
            for (int i = 0; i < header.size(); i++) {
                String h = header.get(i);
                if (h.equals(a) || h.contains(a)) return i;
            }
        }
        return -1;
    }

    private static List<String> splitCsvRow(String row) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQ = false;
        for (int i = 0; i < row.length(); i++) {
            char ch = row.charAt(i);
            if (ch == '"') {
                inQ = !inQ;
                continue;
            }
            if (ch == ',' && !inQ) {
                out.add(cur.toString().trim());
                cur.setLength(0);
                continue;
            }
            cur.append(ch);
        }
        out.add(cur.toString().trim());
        return out;
    }

    private static BigDecimal parseNum(String v) {
        if (v == null || v.isBlank()) return BigDecimal.ZERO;
        String n = v.replaceAll("[^\\d.-]", "");
        if (n.isBlank()) return BigDecimal.ZERO;
        try {
            return new BigDecimal(n);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static LocalDate parseDate(String v) {
        if (v == null || v.isBlank()) return null;
        // dd/MM/yyyy
        if (v.matches("^\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{4}$")) {
            String[] p = v.split("[/\\-.]");
            try {
                return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
            } catch (Exception ignored) {
                return null;
            }
        }
        if (v.length() >= 10 && v.matches("^\\d{4}-\\d{2}-\\d{2}.*")) {
            try {
                return LocalDate.parse(v.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }
}
