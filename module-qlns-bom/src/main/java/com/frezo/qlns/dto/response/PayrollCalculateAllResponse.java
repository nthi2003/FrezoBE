package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả tính lương hàng loạt — FE modal RESULT dùng {@code skippedCount}/{@code errors}.
 */
@Data
@Builder
public class PayrollCalculateAllResponse {

    private Integer month;
    private Integer year;

    /** Số NV tính thành công. */
    private Integer successCount;

    /** Số NV bỏ qua (không có HĐ activated/ACTIVE). */
    private Integer skippedCount;

    /** Số NV lỗi kỹ thuật / nghiệp vụ khác. */
    private Integer errorCount;

    /** Tổng personId đã xét (unique). */
    private Integer totalCandidates;

    @Builder.Default
    private List<ItemError> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Data
    @Builder
    public static class ItemError {
        private String personId;
        /** Tên NV — FE banner/modal skip list. */
        private String personName;
        /** Mã NV (Person.code). */
        private String personCode;
        /**
         * Mã lý do chuẩn hoặc message kỹ thuật.
         * Skip thiếu HĐ: {@code NO_ACTIVE_CONTRACT}.
         */
        private String reason;
    }
}
