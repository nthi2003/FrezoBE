package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.DepreciationScheduleRequest;
import com.frezo.qtbv.dto.response.DepreciationPostingResponse;
import com.frezo.qtbv.dto.response.DepreciationScheduleResponse;

import java.util.List;

/**
 * Quản lý lịch khấu hao TSCĐ + post GL định kỳ.
 * <p>Post GL đi qua {@code JournalService.createAndPost} với {@code source=DEPRECIATION}
 * và {@code idempotencyKey="DEP-YYYY-MM"} → chạy lại cùng kỳ không double-post.
 */
public interface DepreciationService {

    /** Sinh lịch khấu hao cho 1 asset. Idempotent — nếu đã có ACTIVE → throw. */
    DepreciationScheduleResponse generateSchedule(DepreciationScheduleRequest req);

    /** Danh sách lịch khấu hao (optional filter assetId). */
    List<DepreciationScheduleResponse> listSchedules(String assetId);

    /** Post khấu hao 1 kỳ (year/month). Trả về DepreciationPosting đã POSTED. */
    DepreciationPostingResponse postPeriod(int year, int month);

    /** Preview số tiền sẽ post (không ghi GL). */
    DepreciationPostingResponse previewPeriod(int year, int month);

    /** Danh sách / chi tiết posting theo kỳ. */
    List<DepreciationPostingResponse> listPostings(Integer year, Integer month);
}
