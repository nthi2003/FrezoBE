package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Payload public từ landing page (form đăng ký / liên hệ) — KHÔNG cần auth.
 * <p>
 * Anti-spam:
 * <ul>
 *   <li>Field <b>_hp</b> = honeypot; nếu bot điền vào thì reject.</li>
 *   <li>Field <b>_ts</b> = timestamp millis khi mở form; server check delta &gt; 2s
 *       (bot thường submit ngay lập tức).</li>
 * </ul>
 * Rate limit theo IP được apply ở controller.
 */
@Data
public class PublicLeadRequest {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 200, message = "Họ tên tối đa 200 ký tự")
    private String name;

    /**
     * SĐT hoặc email — bắt buộc ít nhất 1 (validate ở controller/service).
     */
    @Size(max = 50, message = "SĐT tối đa 50 ký tự")
    private String phone;

    @Email(message = "Email không hợp lệ")
    @Size(max = 200, message = "Email tối đa 200 ký tự")
    private String email;

    @Size(max = 255, message = "Chủ đề tối đa 255 ký tự")
    private String subject;

    @Size(max = 2000, message = "Nội dung tối đa 2000 ký tự")
    private String message;

    // ---- Anti-spam ----
    /** Honeypot — form thật hidden field này; bot sẽ điền. */
    private String _hp;

    /** Millis khi form mount (frontend set). */
    private Long _ts;
}
