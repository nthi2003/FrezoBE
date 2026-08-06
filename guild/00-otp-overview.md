# Frezo Backend — OTP Forgot Password Overview

> Guide **chuyên đề** cross-module: luồng OTP **quên mật khẩu** (không phải tài liệu full từng module).
> OTP là **subset** của auth + notification + email + QTHT lock — xem full module tại các link mục 8.
> Index guild: [README.md](./README.md). Chuẩn trình bày: [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

Context path HTTP thường là `/api` (ví dụ `POST /api/auth/forgot-password`).

**Phạm vi:** Forgot password OTP thôi — **không** gồm 2FA login `/auth/verify-otp`.

---

## 1. Nghiệp vụ

User quên mật khẩu → nhập email → nhận **OTP 6 số** qua email → nhập OTP đúng → nhận `resetToken` → đặt mật khẩu mới.

| Yêu cầu | Hành vi hệ thống |
|---------|------------------|
| Không lộ email tồn tại | Bước 1 luôn trả success (kể cả email lạ) |
| OTP chỉ chủ email thấy | Plaintext OTP **chỉ** trong body mail; DB lưu hash |
| Chống dò mã | Sai tối đa 5 lần → khóa tài khoản + blacklist IP |
| Chống spam gửi | Cooldown 60 giây giữa 2 lần xin OTP cùng email |

---

## 2. Ba bước API (public)

| # | Method | Path | Params | Kết quả |
|---|--------|------|--------|---------|
| 1 | `POST` | `/auth/forgot-password` | `email` | Message: nếu email tồn tại thì đã gửi OTP |
| 2 | `POST` | `/auth/verify-reset-otp` | `email`, `otp` | `data` = `resetToken` (dùng 1 lần) |
| 3 | `POST` | `/auth/reset-password` | `email`, `resetToken`, `newPassword` | Đổi MK thành công |

**Không nhầm với:**

| Endpoint | Mục đích | Service |
|----------|----------|---------|
| `/auth/verify-otp` | OTP **2FA login** | `AuthTwoFactorService` |
| `/auth/verify-reset-otp` | OTP **quên mật khẩu** | `AuthPasswordResetService` |

---

## 3. Call chain tổng quát

```
[FE] ForgotPasswordPage
  → authApi.forgotPassword / verifyResetOtp / resetPassword
       │
       ▼
AuthController                          (module-auth-bom)
  → AuthService / AuthServiceImpl
       → AuthPasswordResetService       ★ sinh OTP / verify / reset / lock
            │
            ├─ UserRepository           users.reset_key, reset_date, status, password
            │
            ├─ NotificationService ───► NotificationServiceImpl   (module-server)
            │                                └─ EmailService ──► EmailServiceImpl  (module-email-bom)
            │                                                         └─ SMTP + send_emails
            │
            ├─ IpBlockService ────────► IpBlockServiceImpl        (module-qtht-bom)
            │                                └─ IpBlacklistService → ip_blacklist
            │
            └─ AuditLogService          (module-common)
```

Contracts → [module-common.md](./module-common.md). Chi tiết auth (kèm login/2FA/session) → [module-auth-bom.md](./module-auth-bom.md) §8.

---

## 4. OTP được sinh ra thế nào? (tóm tắt)

| Hạng mục | Giá trị |
|----------|---------|
| Class | `AuthPasswordResetService` |
| Method | `generateOtp()` |
| RNG | `java.security.SecureRandom` |
| Công thức | `String.format("%06d", SECURE_RANDOM.nextInt(1_000_000))` |
| Phạm vi | `000000` … `999999` |
| Lưu DB | `"OTP:" + SHA-256(otp)` vào `users.reset_key` |
| TTL | 10 phút (`users.reset_date`) |
| Log | **Không** log mã OTP ở bất kỳ level nào |

Sau verify đúng: `reset_key = "TOK:" + SHA-256(resetToken)`; client dùng `resetToken` gọi reset password.

---

## 5. Hằng số bảo mật

| Constant | Value | Ý nghĩa |
|----------|-------|---------|
| `OTP_TTL_MINUTES` | `10` | Hạn OTP |
| `RESET_TOKEN_TTL_MINUTES` | `10` | Hạn `resetToken` sau verify |
| `MAX_VERIFY_ATTEMPTS` | `5` | Sai OTP → khóa |
| `RESEND_COOLDOWN_SECONDS` | `60` | Cooldown gửi lại |
| `MIN_PASSWORD_LENGTH` | `6` | Độ dài MK mới tối thiểu |
| Prefix `OTP:` / `TOK:` | — | Phân tách 2 giai đoạn trong `reset_key` |

---

## 6. Admin quan sát ở đâu?

| Cần xem | Bảng / API |
|---------|------------|
| Mail OTP SUCCESS / FAILED | `send_emails` (`type=EMAIL`) — `GET /email/send/log` |
| IP bị chặn | `ip_blacklist` — `/qtht/ip-blacklist` |
| Sự kiện khóa do OTP | Audit — action `OTP_BRUTE_FORCE_LOCK` |

---

## 7. Checklist đọc code lần đầu

- [ ] Mở `AuthController` — 3 endpoint forgot / verify-reset / reset
- [ ] Mở `AuthPasswordResetService.generateOtp()` + `forgotPassword()`
- [ ] Theo `notifyUserWithEmailFallback` → `NotificationServiceImpl.trySendUrgentEmail`
- [ ] Mở `EmailServiceImpl.sendByTemplate("URGENT_NOTIFICATION", …)`
- [ ] Mở `lockAfterBruteForce` → `IpBlockService.lockUserAndBlacklistIp` + `AuditLogService.logAction`
- [ ] Xác nhận auth chỉ inject interface common — không import email/qtht impl

---

## 8. Full module docs (OTP chỉ là một phần)

| File | Module | OTP nằm ở đâu trong doc full |
|------|--------|------------------------------|
| [module-auth-bom.md](./module-auth-bom.md) | auth | §8 OTP quên MK (cạnh login / 2FA / session) |
| [module-server.md](./module-server.md) | server | §5 Notification — nhánh urgent email |
| [module-email-bom.md](./module-email-bom.md) | email | §8 Consumer OTP |
| [module-qtht-bom.md](./module-qtht-bom.md) | qtht | Khóa + `ip_blacklist` |
| [module-common.md](./module-common.md) | common | §9 Contracts OTP dùng |

Thứ tự đọc OTP: **overview (file này) → auth §8 → common §9 → server §5 → email §8 → qtht**.

---

*Cập nhật khi đổi số bước API, TTL, hoặc chỗ ghi log/blacklist. Đổi full module → sửa file `module-*.md` tương ứng, giữ link mục 8.*
