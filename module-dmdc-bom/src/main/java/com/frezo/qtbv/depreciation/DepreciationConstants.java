package com.frezo.qtbv.depreciation;

public final class DepreciationConstants {

    private DepreciationConstants() {}

    public static final String METHOD_STRAIGHT_LINE = "STRAIGHT_LINE";
    public static final String METHOD_DECLINING = "DECLINING";

    public static final String SCHEDULE_ACTIVE = "ACTIVE";
    public static final String SCHEDULE_DONE = "DONE";
    public static final String SCHEDULE_CANCELLED = "CANCELLED";

    public static final String POSTING_POSTED = "POSTED";
    public static final String POSTING_REVERSED = "REVERSED";
    public static final String POSTING_FAILED = "FAILED";

    /** TT133: 642 = Chi phí quản lý DN (khấu hao). */
    public static final String DEFAULT_DEPRECIATION_EXPENSE = "642";
    /** TT133: 214 = Hao mòn TSCĐ. */
    public static final String DEFAULT_ACCUMULATED_DEPRECIATION = "214";

    public static String idempotencyKey(int year, int month) {
        return String.format("DEP-%04d-%02d", year, month);
    }
}
