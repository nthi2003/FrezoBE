package com.frezo.common.scheduling;


public interface SchedulableJob {

    /** Mã job duy nhất, UPPER_SNAKE — VD {@code DB_BACKUP}. */
    String getCode();

    /** Tên hiển thị tiếng Việt cho màn hình quản trị. */
    String getDisplayName();

    /** Mô tả ngắn job làm gì. */
    String getDescription();

    /** Module sở hữu job — VD {@code QTHT}, {@code WAREHOUSE}. */
    String getModuleCode();

    /** Cron mặc định (Spring 6 field, có giây) dùng khi seed lần đầu. */
    String getDefaultCron();

    default String checkReadiness() {
        return null;
    }

    /** Thân job. Throw để scheduler ghi nhận lần chạy FAILED. */
    void execute() throws Exception;
}
