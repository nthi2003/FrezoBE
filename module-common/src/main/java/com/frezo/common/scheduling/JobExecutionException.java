package com.frezo.common.scheduling;

/**
 * Lỗi tác vụ nền đã có thông điệp tiếng Việt dành cho quản trị viên.
 * <p>
 * Scheduler lấy nguyên {@link #getMessage()} làm dòng đầu của lịch sử chạy thay vì ghép
 * tên class exception — chỉ dùng khi thông điệp thật sự giải thích được cho người dùng
 * (VD thiếu công cụ, thiếu cấu hình). Lỗi kỹ thuật ngoài dự kiến cứ để exception gốc ném ra.
 */
public class JobExecutionException extends RuntimeException {

    public JobExecutionException(String message) {
        super(message);
    }

    public JobExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
