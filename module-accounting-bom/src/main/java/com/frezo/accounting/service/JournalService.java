package com.frezo.accounting.service;

import com.frezo.accounting.common.PostingSource;
import com.frezo.accounting.dto.request.JournalEntryRequest;
import com.frezo.accounting.dto.response.JournalEntryResponse;

import java.util.List;

/**
 * Ghi & quản lý chứng từ kế toán. Là "cửa duy nhất" để mọi module (Payroll, CRM, WMS...)
 * đưa dữ liệu vào GL.
 */
public interface JournalService {

    /**
     * Tạo chứng từ ở trạng thái DRAFT (chưa vào GL). Có thể edit.
     */
    JournalEntryResponse createDraft(JournalEntryRequest req);

    /**
     * Tạo chứng từ và POST luôn (production use). Trả về entry đã POSTED.
     * <p>Idempotent qua {@code idempotencyKey}: nếu đã có entry với key trùng, trả entry cũ.
     */
    JournalEntryResponse createAndPost(JournalEntryRequest req);

    /**
     * Post chứng từ DRAFT vào GL (đổi status = POSTED).
     */
    JournalEntryResponse post(String id);

    /**
     * Đảo chứng từ (tạo entry ngược, không xoá). Trả về entry đảo mới.
     */
    JournalEntryResponse reverse(String id, String reason);

    JournalEntryResponse getById(String id);

    List<JournalEntryResponse> listByPeriod(String periodId);

    List<JournalEntryResponse> listBySource(PostingSource source, String sourceId);
}
