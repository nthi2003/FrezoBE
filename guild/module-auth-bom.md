# Frezo Backend — module-auth-bom (Authentication & Session)

> Module sở hữu **đăng nhập, 2FA OTP, JWT refresh, session/history, profile/avatar**, và **quên mật khẩu OTP**.
> Đọc cùng [README.md](./README.md) · [00-otp-overview.md](./00-otp-overview.md) (chuyên sâu OTP quên MK) · [module-server.md](./module-server.md) · [module-email-bom.md](./module-email-bom.md) · [module-qtht-bom.md](./module-qtht-bom.md) · [module-common.md](./module-common.md).
> Chuẩn trình bày giống [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

Package gốc: `com.frezo.auth`. Context path HTTP thường là `/api` (ví dụ `POST /api/auth/login`).

---

## 1. Phạm vi module

| Nhóm | Endpoint chính | Service chuyên biệt |
|------|----------------|---------------------|
| Login / refresh | `/auth/login`, `/auth/refresh-token` | `AuthLoginProcessor` |
| 2FA login | `/auth/verify-otp` | `AuthTwoFactorService` |
| Quên mật khẩu OTP | `/auth/forgot-password`, `/verify-reset-otp`, `/reset-password` | `AuthPasswordResetService` |
| Profile | `/auth/profile`, `/auth/avatar` | `AuthProfileService` |
| Logout / history | `/auth/logout`, `/auth/login-history` | `AuthSessionService` |
| Session quản lý | `/auth/session/**` | `UserSessionService` |
| Thống kê | `/auth/statistic/**` | `UserActivityService` |

Façade: `AuthService` / `AuthServiceImpl` — **không** nhét logic dài; delegate sang package `service.impl.auth`.

---

## 2. Class map

| Class | Package | Vai trò |
|-------|---------|---------|
| `AuthController` | `controller` | Login, 2FA, forgot/reset, profile, logout, history, refresh |
| `SessionController` | `controller` | Active sessions, revoke, heartbeat, online count |
| `UserActivityController` | `controller` | Login-by-day, usage summary |
| `AuthServiceImpl` | `service.impl` | Façade 5 deps |
| `AuthLoginProcessor` | `service.impl.auth` | Login + refresh token |
| `AuthTwoFactorService` | `service.impl.auth` | OTP 2FA (`otp_code`) |
| `AuthPasswordResetService` | `service.impl.auth` | ★ OTP quên MK (`reset_key`) |
| `AuthSessionService` | `service.impl.auth` | Logout + login history |
| `AuthProfileService` | `service.impl.auth` | Profile + avatar MinIO |
| `AuthTokenBuilder` | `service.impl.auth` | Sinh JWT access/refresh |
| `IpResolver` | `service.impl.auth` | Lấy IP client (static) |
| `User` | `entity` | Bảng `users` |
| `UserSession` | `entity` | Bảng session active |
| `LoginHistory` | `entity` | Bảng `login_history` |
| `TokenBlacklist` | `entity` | Token đã logout |
| `UserRole` | `entity` | Gán role |

---

## 3. API — AuthController `/auth`

| Method | Path | Auth? | Hành vi |
|--------|------|-------|---------|
| `POST` | `/auth/login` | Public | Username/password → JWT hoặc yêu cầu 2FA |
| `POST` | `/auth/verify-otp` | Public | Xác thực OTP **2FA login** |
| `POST` | `/auth/forgot-password` | Public | Xin OTP quên MK (email) |
| `POST` | `/auth/verify-reset-otp` | Public | Verify OTP → `resetToken` |
| `POST` | `/auth/reset-password` | Public | Đặt MK mới bằng `resetToken` |
| `POST` | `/auth/refresh-token` | Public* | Đổi access token từ refresh |
| `GET` | `/auth/login-history` | JWT | Lịch sử đăng nhập |
| `GET` | `/auth/profile` | JWT | Profile user hiện tại |
| `POST` | `/auth/avatar` | JWT | Upload avatar (multipart) |
| `POST` | `/auth/logout` | JWT | Blacklist token + revoke session |

\* Refresh thường gửi refresh token trong body/header — không dùng access JWT hết hạn.

---

## 4. Login & JWT

### 4.1 Luồng login (`AuthLoginProcessor`)

```
POST /auth/login { username, password }
  → validate credential (PasswordEncoder)
  → check status active, IP block (IpBlockService)
  → nếu requires_two_factor:
        sinh OTP → lưu users.otp_code / otp_expiration
        gửi notify/email (tuỳ impl)
        trả LoginResponse yêu cầu 2FA (chưa đủ JWT full)
  → else:
        AuthTokenBuilder sinh access + refresh
        tạo UserSession, ghi LoginHistory SUCCESS
        clear failed attempts
```

| Kết quả | Ý nghĩa |
|---------|---------|
| JWT full | Đăng nhập xong |
| 2FA required | Client gọi `/auth/verify-otp` |
| FAILED | Sai MK → `handleFailedAttempt` / rate limit |

### 4.2 Refresh

`POST /auth/refresh-token` → validate refresh còn hạn / session active → cấp cặp token mới (hoặc access mới).

### 4.3 Logout

`AuthSessionService.logout`: đưa access token vào `TokenBlacklist`, deactivate `UserSession`.

---

## 5. 2FA login (`AuthTwoFactorService`)

| Hạng mục | Chi tiết |
|----------|----------|
| Endpoint | `POST /auth/verify-otp` — params `username` + `code` |
| Cột DB | `users.otp_code` (plaintext), `otp_expiration` |
| TTL | ~5 phút |
| Phạm vi mã | `100000`–`999999` (không pad leading zero như quên MK) |
| Response | `LoginResponse` (JWT) |

✅ Dùng cho bước 2 sau login.
❌ Không dùng cho quên mật khẩu — xem mục 8 và [00-otp-overview.md](./00-otp-overview.md).

---

## 6. Session & lịch sử

### 6.1 Entity `UserSession`

| Field | Ý nghĩa |
|-------|---------|
| `username` | Chủ session |
| `token` / `refreshToken` | JWT lưu để revoke |
| `ipAddress`, `userAgent`, `deviceInfo`, `location` | Metadata thiết bị |
| `loginTime`, `lastActiveTime`, `expiresAt` | Vòng đời |
| `isActive`, `revokedAt`, `revokedBy` | Trạng thái revoke |

### 6.2 SessionController — `/auth/session`

| Method | Path | Hành vi |
|--------|------|---------|
| `GET` | `/auth/session/active` | Session đang active của tôi |
| `GET` | `/auth/session/active/paged` | Phân trang |
| `POST` | `/auth/session/revoke/{id}` | Revoke 1 session |
| `POST` | `/auth/session/revoke-all` | Revoke tất cả (trừ hiện tại tuỳ impl) |
| `GET` | `/auth/session/count` | Đếm session |
| `POST` | `/auth/session/heartbeat` | Cập nhật `lastActiveTime` |
| `GET` | `/auth/session/online-count` | Số user online |
| `GET` | `/auth/session/admin/active` | Admin xem session hệ thống |

### 6.3 `LoginHistory` → `login_history`

| Field | Ý nghĩa |
|-------|---------|
| `userName` | Username |
| `ipAddress`, `userAgent` | Client |
| `loginTime` | Thời điểm |
| `status` | `SUCCESS` / `FAILED` / `2FA_REQUIRED` |

`GET /auth/login-history` — danh sách gần đây của user.

### 6.4 Thống kê — `/auth/statistic`

| Method | Path |
|--------|------|
| `GET` | `/auth/statistic/login-by-day` |
| `GET` | `/auth/statistic/usage-summary` |

---

## 7. Profile & avatar

`AuthProfileService`:

| API | Hành vi |
|-----|---------|
| `GET /auth/profile` | Profile user hiện tại (roles, person link, …) |
| `POST /auth/avatar` | Multipart → MinIO (`MinioService` common) → cập nhật URL avatar |

---

## 8. OTP quên mật khẩu (section chuyên sâu)

> Chi tiết đầy đủ + checklist: [00-otp-overview.md](./00-otp-overview.md). Dưới đây là bản nhúng trong module auth.

### 8.1 Ba API

| # | Path | Params | Kết quả |
|---|------|--------|---------|
| 1 | `POST /auth/forgot-password` | `email` | Success chung (không lộ email) |
| 2 | `POST /auth/verify-reset-otp` | `email`, `otp` | `data` = `resetToken` |
| 3 | `POST /auth/reset-password` | `email`, `resetToken`, `newPassword` | Đổi MK |

### 8.2 Phân biệt 2FA vs quên MK

| | Quên mật khẩu | 2FA login |
|--|---------------|-----------|
| Endpoint | `/auth/verify-reset-otp` | `/auth/verify-otp` |
| Service | `AuthPasswordResetService` | `AuthTwoFactorService` |
| Cột | `reset_key` / `reset_date` | `otp_code` / `otp_expiration` |
| Lưu mã | `"OTP:"` + SHA-256 | Plaintext |
| TTL | 10 phút | ~5 phút |
| Phạm vi | `000000`–`999999` (`%06d`) | `100000`–`999999` |

### 8.3 Sinh OTP & hash

```java
String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
user_key = "OTP:" + sha256(otp);   // sau forgot
reset_key = "TOK:" + sha256(resetToken); // sau verify
```

| Constant | Value |
|----------|-------|
| `OTP_TTL_MINUTES` | `10` |
| `RESET_TOKEN_TTL_MINUTES` | `10` |
| `MAX_VERIFY_ATTEMPTS` | `5` |
| `RESEND_COOLDOWN_SECONDS` | `60` |
| `MIN_PASSWORD_LENGTH` | `6` |

### 8.4 Brute-force

Sau 5 lần sai: clear OTP → `IpBlockService.lockUserAndBlacklistIp(..., OTP_BRUTE_FORCE, null)` → audit `OTP_BRUTE_FORCE_LOCK`.  
Chi tiết → [module-qtht-bom.md](./module-qtht-bom.md).

### 8.5 Gửi mail

`NotificationService.notifyUserWithEmailFallback(..., urgent=true)` → [module-server.md](./module-server.md) → [module-email-bom.md](./module-email-bom.md).

Memory maps in-process: `verifyAttempts`, `lastOtpSentAt` (mất khi restart / multi-instance cần lưu ý).

---

## 9. Entity `User` — cột auth quan trọng

| Cột | Vai trò |
|-----|---------|
| `user_name`, `password`, `email` | Credential |
| `status` | `1` = active |
| `requires_two_factor` | Bật 2FA |
| `otp_code`, `otp_expiration` | 2FA |
| `reset_key` (128), `reset_date` | Quên MK (hash OTP/token) |
| Avatar / profile fields | Profile service |

Repo: `findByEmailIgnoreCase` — bắt buộc cho OTP quên MK.

---

## 10. Dependencies cross-module

```
AuthPasswordResetService / AuthLoginProcessor
  ├─ NotificationService     → impl module-server
  ├─ IpBlockService          → impl module-qtht-bom
  ├─ AuditLogService         → module-common
  ├─ PasswordEncoder         → Spring Security (server config)
  └─ MinioService (avatar)   → module-common
```

| Rule | |
|------|--|
| ✅ Auth chỉ inject interface common | Không import `com.frezo.email.*` / `IpBlacklistService` |
| ❌ Auth tự SMTP | Phá ranh module |

---

## 11. Checklist đọc code

- [ ] `AuthServiceImpl` façade → 5 component `impl.auth`
- [ ] `AuthController` — login / 2FA / forgot / refresh / profile / logout
- [ ] `SessionController` — revoke / heartbeat
- [ ] Phân biệt `/verify-otp` vs `/verify-reset-otp`
- [ ] `AuthPasswordResetService` — hash `OTP:`/`TOK:`, lock 5 lần
- [ ] `UserSession` + `LoginHistory` + `TokenBlacklist`
- [ ] Cross: [00-otp-overview.md](./00-otp-overview.md), [module-server.md](./module-server.md), [module-email-bom.md](./module-email-bom.md), [module-qtht-bom.md](./module-qtht-bom.md)

---

*Cập nhật khi đổi JWT claim, 2FA TTL, session model, hoặc OTP quên MK.*
