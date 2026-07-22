package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.response.LeadImportBatchResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * LeadImportService — nhập lead hàng loạt từ CSV/Excel.
 * <p>
 * Format cột chấp nhận (không phân biệt hoa/thường, dấu):
 * <pre>
 *   name, phone, email, address, subject, message, source
 * </pre>
 * Bắt buộc tối thiểu: `phone` HOẶC `email` (để dedupe được).
 */
public interface LeadImportService {

    /**
     * Import 1 file lead (CSV hoặc XLSX).
     *
     * @param file     file upload
     * @param source   nguồn mặc định gán cho các lead trong file (VD "LANDING_2026_Q1")
     * @param dedupe   nếu true → skip lead trùng phone/email đã tồn tại; false = luôn insert
     * @return summary batch (số dòng success/skip/fail)
     */
    LeadImportBatchResponse importLeads(MultipartFile file, String source, boolean dedupe);

    /** Rollback batch: xóa mềm toàn bộ lead có importBatchId = batchId. */
    void rollback(String batchId);

    /** Preview 20 dòng đầu để user check format trước khi import. */
    List<List<String>> preview(MultipartFile file);

    /** Danh sách batch đã upload (audit trail). */
    List<LeadImportBatchResponse> history();
}
