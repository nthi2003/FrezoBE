package com.frezo.common.scheduling;

/**
 * Hợp đồng cho mọi tác vụ nền chạy theo lịch cấu hình được trong DB (bảng {@code system_job}).
 * <p>
 * Bean implement interface này sẽ được {@code DynamicJobScheduler} (module-qtht) tự động:
 * <ul>
 *   <li>Seed một dòng {@code system_job} theo {@link #getDefaultCron()} nếu chưa tồn tại.</li>
 *   <li>Đăng ký {@code CronTrigger} theo cron đang lưu trong DB (không phải default).</li>
 *   <li>Ghi lịch sử chạy vào {@code system_job_history}.</li>
 * </ul>
 * <b>Không</b> dùng {@code @Scheduled} trên bean implement interface này — cron do DB quyết định.
 */
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

    /**
     * Kiểm tra điều kiện cần trước khi chạy (công cụ ngoài, tệp cấu hình, khoá API...).
     * <p>
     * Trả {@code null} khi sẵn sàng; ngược lại trả câu tiếng Việt giải thích thiếu gì và
     * cách khắc phục — màn hình quản trị hiển thị "Chưa cấu hình" kèm câu này thay vì để
     * người dùng bấm chạy rồi nhận lỗi.
     * <p>
     * Phải nhẹ (không gọi mạng, không sinh tiến trình) vì được gọi mỗi lần tải danh sách job.
     */
    default String checkReadiness() {
        return null;
    }

    /** Thân job. Throw để scheduler ghi nhận lần chạy FAILED. */
    void execute() throws Exception;
}
