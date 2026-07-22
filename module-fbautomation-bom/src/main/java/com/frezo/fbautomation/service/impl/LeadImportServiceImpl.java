package com.frezo.fbautomation.service.impl;

import com.frezo.fbautomation.dto.response.LeadImportBatchResponse;
import com.frezo.fbautomation.entity.FacebookLead;
import com.frezo.fbautomation.entity.LeadImportBatch;
import com.frezo.fbautomation.repository.FacebookLeadRepository;
import com.frezo.fbautomation.repository.LeadImportBatchRepository;
import com.frezo.fbautomation.service.LeadImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * LeadImportServiceImpl — parse CSV/XLSX, dedupe theo phone/email, insert batch.
 * <p>
 * Design choices:
 * - Không dùng OpenCSV: manual split để tránh thêm dep, đủ dùng cho lead file <10k dòng.
 * - Dedupe query 1 lần trước loop → tránh N+1 với file to.
 * - Error log JSON hóa manually (không cần Jackson) — tối đa 100 dòng lỗi đầu để không nổ TEXT column.
 * - Rollback = soft-delete (đặt is_deleted=true) qua {@link FacebookLead}'s BaseEntity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadImportServiceImpl implements LeadImportService {

    private static final int PREVIEW_ROWS  = 20;
    private static final int MAX_ERROR_LOG = 100;

    /** Header aliases — cho phép user upload file với tên cột tiếng Việt hoặc tiếng Anh. */
    private static final Map<String, String> HEADER_ALIAS = Map.ofEntries(
            Map.entry("name",       "name"),
            Map.entry("hoten",      "name"),
            Map.entry("hovaten",    "name"),
            Map.entry("fullname",   "name"),
            Map.entry("phone",      "phone"),
            Map.entry("sdt",        "phone"),
            Map.entry("dienthoai",  "phone"),
            Map.entry("mobile",     "phone"),
            Map.entry("email",      "email"),
            Map.entry("mail",       "email"),
            Map.entry("address",    "address"),
            Map.entry("diachi",     "address"),
            Map.entry("subject",    "subject"),
            Map.entry("chude",      "subject"),
            Map.entry("service",    "subject"),
            Map.entry("dichvu",     "subject"),
            Map.entry("message",    "message"),
            Map.entry("noidung",    "message"),
            Map.entry("note",       "message"),
            Map.entry("ghichu",     "message"),
            Map.entry("source",     "source"),
            Map.entry("nguon",      "source")
    );

    private final FacebookLeadRepository leadRepository;
    private final LeadImportBatchRepository batchRepository;

    // ============================================================
    //  IMPORT
    // ============================================================
    @Override
    @Transactional
    public LeadImportBatchResponse importLeads(MultipartFile file, String source, boolean dedupe) {
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed");
        List<List<String>> rows;
        try {
            rows = readAllRows(file);
        } catch (Exception ex) {
            log.error("Không parse được file lead: {}", filename, ex);
            throw new IllegalArgumentException("Không đọc được file: " + ex.getMessage());
        }
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("File rỗng — không có dòng nào để import");
        }

        // Row 0 = header. Map header index → tên chuẩn (name/phone/email/…)
        List<String> header = rows.get(0);
        Map<Integer, String> colMap = mapHeaders(header);
        if (!colMap.containsValue("phone") && !colMap.containsValue("email")) {
            throw new IllegalArgumentException("File phải có ít nhất 1 cột 'phone' hoặc 'email' để dedupe");
        }

        // ---- Pre-load existing phone/email để dedupe O(1) ----
        // Với file 10k dòng và DB có <100k lead, load toàn bộ vẫn nhẹ hơn N query.
        var allExisting = leadRepository.findAll(); // TODO Spec filter theo phone/email in-set khi >100k lead
        var existingPhones = new java.util.HashSet<String>();
        var existingEmails = new java.util.HashSet<String>();
        for (var l : allExisting) {
            if (l.getPhone() != null) existingPhones.add(normalizePhone(l.getPhone()));
            if (l.getEmail() != null) existingEmails.add(l.getEmail().toLowerCase(Locale.ROOT).trim());
        }

        int success = 0, skipped = 0, failed = 0;
        StringBuilder errorLog = new StringBuilder("[");
        String username = currentUsername();

        // Tạo batch trước để có ID gán cho từng lead con.
        LeadImportBatch batch = batchRepository.save(LeadImportBatch.builder()
                .filename(filename)
                .source(source != null ? source : "MANUAL_IMPORT")
                .rowCount(rows.size() - 1)
                .uploadedBy(username)
                .uploadedAt(OffsetDateTime.now())
                .build());

        List<FacebookLead> toSave = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            try {
                Map<String, String> data = extractRow(row, colMap);
                String phone = normalizePhone(data.get("phone"));
                String email = data.get("email") != null ? data.get("email").toLowerCase(Locale.ROOT).trim() : null;

                if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
                    failed++;
                    appendError(errorLog, i + 1, "Thiếu cả phone và email");
                    continue;
                }
                if (dedupe && (
                        (phone != null && existingPhones.contains(phone)) ||
                        (email != null && existingEmails.contains(email)))) {
                    skipped++;
                    continue;
                }

                FacebookLead lead = FacebookLead.builder()
                        .name(data.get("name"))
                        .phone(phone)
                        .email(email)
                        .address(data.get("address"))
                        .subject(data.get("subject"))
                        .message(data.get("message"))
                        .source(data.getOrDefault("source", batch.getSource()))
                        .status("NEW")
                        .importBatchId(batch.getId())
                        .build();
                toSave.add(lead);
                if (phone != null) existingPhones.add(phone);
                if (email != null) existingEmails.add(email);
                success++;
            } catch (Exception ex) {
                failed++;
                appendError(errorLog, i + 1, ex.getMessage());
            }
        }
        if (!toSave.isEmpty()) leadRepository.saveAll(toSave);

        // Trim errorLog cuối để đúng JSON.
        if (errorLog.charAt(errorLog.length() - 1) == ',') {
            errorLog.deleteCharAt(errorLog.length() - 1);
        }
        errorLog.append("]");

        batch.setSuccessCount(success);
        batch.setSkippedCount(skipped);
        batch.setFailedCount(failed);
        batch.setErrorLog(errorLog.toString());
        batchRepository.save(batch);

        log.info("Lead import: file={} rows={} ok={} skip={} fail={}", filename, rows.size() - 1, success, skipped, failed);
        return toResponse(batch);
    }

    @Override
    @Transactional
    public void rollback(String batchId) {
        LeadImportBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch không tồn tại: " + batchId));
        if (Boolean.TRUE.equals(batch.getRolledBack())) return;

        var leads = leadRepository.findAll().stream()
                .filter(l -> batchId.equals(l.getImportBatchId()))
                .toList();
        for (var l : leads) l.setIsDeleted(true);
        leadRepository.saveAll(leads);

        batch.setRolledBack(true);
        batchRepository.save(batch);
        log.info("Rolled back batch {} — soft-deleted {} leads", batchId, leads.size());
    }

    @Override
    public List<List<String>> preview(MultipartFile file) {
        try {
            var rows = readAllRows(file);
            return rows.subList(0, Math.min(rows.size(), PREVIEW_ROWS + 1)); // +1 vì có header
        } catch (Exception ex) {
            throw new IllegalArgumentException("Không đọc được file: " + ex.getMessage());
        }
    }

    @Override
    public List<LeadImportBatchResponse> history() {
        return batchRepository.findAll().stream()
                .sorted((a, b) -> {
                    var au = a.getUploadedAt(); var bu = b.getUploadedAt();
                    if (au == null && bu == null) return 0;
                    if (au == null) return 1; if (bu == null) return -1;
                    return bu.compareTo(au);
                })
                .map(this::toResponse)
                .toList();
    }

    // ============================================================
    //  PARSER
    // ============================================================
    private List<List<String>> readAllRows(MultipartFile file) throws Exception {
        String filename = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        try (InputStream in = file.getInputStream()) {
            if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                return readExcel(in);
            }
            return readCsv(in);
        }
    }

    private List<List<String>> readCsv(InputStream in) throws Exception {
        List<List<String>> out = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                out.add(parseCsvLine(line));
            }
        }
        return out;
    }

    /**
     * Parse CSV theo RFC 4180 giản lược — hỗ trợ quote " và escape "" bên trong quote.
     * Không dùng lib để tránh thêm dep; đủ chính xác cho lead file 10k dòng.
     */
    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuote = false;
                } else cur.append(c);
            } else {
                if (c == ',') { cells.add(cur.toString()); cur.setLength(0); }
                else if (c == '"' && cur.length() == 0) inQuote = true;
                else cur.append(c);
            }
        }
        cells.add(cur.toString());
        return cells;
    }

    private List<List<String>> readExcel(InputStream in) throws Exception {
        List<List<String>> out = new ArrayList<>();
        try (Workbook wb = new XSSFWorkbook(in)) {
            Sheet sheet = wb.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                short last = row.getLastCellNum();
                if (last <= 0) continue;
                for (int c = 0; c < last; c++) {
                    Cell cell = row.getCell(c);
                    cells.add(cell == null ? "" : fmt.formatCellValue(cell));
                }
                if (cells.stream().anyMatch(s -> s != null && !s.isBlank())) out.add(cells);
            }
        }
        return out;
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    private Map<Integer, String> mapHeaders(List<String> header) {
        Map<Integer, String> map = new HashMap<>();
        for (int i = 0; i < header.size(); i++) {
            String key = normalizeHeader(header.get(i));
            String std = HEADER_ALIAS.get(key);
            if (std != null) map.put(i, std);
        }
        return map;
    }

    private String normalizeHeader(String s) {
        if (s == null) return "";
        String noAccent = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return noAccent.toLowerCase(Locale.ROOT)
                .replace("đ", "d")
                .replaceAll("[^a-z0-9]", "");
    }

    private Map<String, String> extractRow(List<String> row, Map<Integer, String> colMap) {
        Map<String, String> data = new HashMap<>();
        for (var e : colMap.entrySet()) {
            if (e.getKey() < row.size()) {
                String v = row.get(e.getKey());
                if (v != null && !v.isBlank()) data.put(e.getValue(), v.trim());
            }
        }
        return data;
    }

    private String normalizePhone(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+84")) digits = "0" + digits.substring(3);
        else if (digits.startsWith("84") && digits.length() >= 11) digits = "0" + digits.substring(2);
        return digits.isBlank() ? null : digits;
    }

    private String currentUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        } catch (Exception ex) { return "system"; }
    }

    private void appendError(StringBuilder buf, int row, String msg) {
        // Tránh log log quá dài — chỉ giữ 100 lỗi đầu.
        long commas = buf.chars().filter(ch -> ch == ',').count();
        if (commas > MAX_ERROR_LOG) return;
        if (buf.length() > 1) buf.append(",");
        buf.append("{\"row\":").append(row).append(",\"msg\":\"")
                .append(msg == null ? "" : msg.replace("\"", "'")).append("\"}");
    }

    private LeadImportBatchResponse toResponse(LeadImportBatch b) {
        LeadImportBatchResponse r = new LeadImportBatchResponse();
        r.setId(b.getId());
        r.setFilename(b.getFilename());
        r.setSource(b.getSource());
        r.setRowCount(b.getRowCount());
        r.setSuccessCount(b.getSuccessCount());
        r.setSkippedCount(b.getSkippedCount());
        r.setFailedCount(b.getFailedCount());
        r.setErrorLog(b.getErrorLog());
        r.setUploadedBy(b.getUploadedBy());
        r.setUploadedAt(b.getUploadedAt());
        r.setRolledBack(b.getRolledBack());
        return r;
    }

    // Suppress unused imports warning for Arrays (used implicitly by tests / future filters).
    @SuppressWarnings("unused")
    private static final List<String> RESERVED = Arrays.asList("");
}
