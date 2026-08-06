# Frezo Backend — module-email-bom (Email config / template / send / inbox)

> Module **email vận hành**: SMTP config, template HTML, gửi bulk / theo group, inbox IMAP, lịch sử `send_emails`.
> **OTP quên mật khẩu** chỉ là một consumer (qua `NotificationService` → `sendByTemplate` / `sendSimple`) — xem mục 8 và [00-otp-overview.md](./00-otp-overview.md).
> Đọc cùng [README.md](./README.md) · [module-server.md](./module-server.md) · [module-auth-bom.md](./module-auth-bom.md) · [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

Package gốc: `com.frezo.email`. Module Maven: `module-email-bom`.

---

## 1. Phạm vi module

| Nhóm | Base path | Nội dung |
|------|-----------|----------|
| Config SMTP | `/email/config` | CRUD, activate/deactivate, test connection |
| Template | `/email/template` | CRUD + send-test |
| Group | `/email/group` | Nhóm recipient |
| Send | `/email/send` | Bulk, by-group, log |
| Inbox | `/email/inbox` | Đọc hộp thư theo config (IMAP) |

**Không thuộc module này:** in-app notification / WebSocket (common + server), CRM email sequence (`module-crm-bom`).

---

## 2. Class map

| Layer | Class | Vai trò |
|-------|-------|---------|
| Service | `EmailService` / `EmailServiceImpl` | SMTP, template, log SUCCESS/FAILED |
| Controller | `EmailConfigController` | `/email/config` |
| Controller | `EmailtemplateController` | `/email/template` |
| Controller | `EmailGroupController` | `/email/group` |
| Controller | `EmailSendController` | `/email/send/bulk`, `/by-group`, `/log` |
| Controller | `EmailInboxController` | `/email/inbox` |
| Entity | `EmailConfig` | Bảng config SMTP |
| Entity | `EmailTemplate` | Mẫu HTML + `code` |
| Entity | `EmailGroup` | Nhóm email |
| Entity | `SendEmail` | Log `send_emails` + recipients |
| Config | `EmailOtpTemplateSeedRunner` | Seed `URGENT_NOTIFICATION`, `PASSWORD_RESET` |
| Error | `EmailErrorCode` | CONFIG_NOT_FOUND, SEND_FAILED, … |

---

## 3. API

### 3.1 Config — `/email/config`

| Method | Path | Hành vi |
|--------|------|---------|
| `GET` | `/email/config` | List |
| `POST` | `/email/config` | Tạo |
| `PUT` | `/email/config/{id}` | Cập nhật |
| `PUT` | `/email/config/{id}/activate` | Bật |
| `PUT` | `/email/config/{id}/deactivate` | Tắt |
| `DELETE` | `/email/config/{id}` | Soft-delete |
| `POST` | `/email/config/{id}/test-connection` | Test SMTP |

**Resolve config khi gửi:** lấy `activated=true` và `isDeleted≠true`, chọn `updatedDate` mới nhất.

| Rule | |
|------|--|
| ✅ Host SMTP không chứa `@` | Tránh nhầm email vào ô host |
| ✅ Password trim khoảng trắng | App password Gmail |
| ❌ Tin thứ tự `findByActivatedTrue()` không ORDER BY | Phải max theo `updatedDate` |

### 3.2 Template — `/email/template`

| Method | Path |
|--------|------|
| CRUD | `/email/template`, `/email/template/{id}` |
| `POST` | `/email/template/{id}/send-test` |

Placeholder: `{{key}}` replace tuần tự trong subject/body.

Template seed thường gặp:

| Code | Dùng cho |
|------|----------|
| `URGENT_NOTIFICATION` | `{{title}}`, `{{content}}` — urgent notify (OTP hiện dùng cái này) |
| `PASSWORD_RESET` | `{{name}}`, `{{otp}}`, `{{minutes}}` — sẵn seed, wire khi caller gọi đúng code |

### 3.3 Group — `/email/group`

CRUD nhóm recipient — `/email/group`, `/email/group/{id}`.

### 3.4 Send — `/email/send`

| Method | Path | Hành vi |
|--------|------|---------|
| `POST` | `/email/send/bulk` | Gửi nhiều recipient + template/params |
| `POST` | `/email/send/by-group` | Gửi theo `EmailGroup` |
| `GET` | `/email/send/log` | Lịch sử phân trang (`SendEmailLogFilter`) |

### 3.5 Inbox — `/email/inbox`

| Method | Path | Hành vi |
|--------|------|---------|
| `GET` | `/email/inbox/{configId}` | List thư |
| `GET` | `/email/inbox/{configId}/{uid}` | Chi tiết |
| `PUT` | `/email/inbox/{configId}/{uid}/read` | Đánh dấu đã đọc |

---

## 4. Gửi mail — `EmailService`

| Method | Ý nghĩa |
|--------|---------|
| `sendByTemplate(code, params, recipients)` | Resolve template + config → SMTP từng recipient |
| `sendSimple(to, subject, html)` | HTML thô, không template |
| `sendEmail(...)` | Low-level MIME |
| `getSendLogs(filter)` | Query `send_emails` |

### 4.1 `sendByTemplate` — các bước

| Bước | Việc | Fail |
|------|------|------|
| 1 | `findByCode(templateCode)` | `EMAIL_TEMPLATE_NOT_FOUND` |
| 2 | `resolveActiveConfig()` | `CONFIG_NOT_FOUND` |
| 3 | `processTemplate` subject + body | — |
| 4 | Loop recipient → SMTP | Gom sent/failed |
| 5–6 | `logSendEmail` SUCCESS và/hoặc FAILED | |
| 7 | `sent` rỗng → throw `SEND_FAILED` | Caller có thể fallback |

### 4.2 Constants `EmailServiceImpl`

| Constant | Value |
|----------|-------|
| `TYPE_EMAIL` | `"EMAIL"` |
| `STATUS_SUCCESS` | `"SUCCESS"` |
| `STATUS_FAILED` | `"FAILED"` |
| `MAX_ERROR_LENGTH` | `1000` |

Timeout SMTP connect/read/write: **10000 ms**. STARTTLS + auth bật.

---

## 5. Log `send_emails`

Entity `SendEmail` → bảng `send_emails` (+ `send_email_recipients`, `send_email_files`).

| Field | Ý nghĩa |
|-------|---------|
| `emailTemplateId` | Null nếu `sendSimple` |
| `topic` | Subject |
| `recipients` | Element collection |
| `type` | `EMAIL` (chừa SMS/ZALO) |
| `status` | `SUCCESS` / `FAILED` |
| `errorMessage` | Truncate 1000 — null khi SUCCESS |
| `description` | Mô tả / context |
| `file` | Attachment paths |

Migration liên quan: `V202608061000__send_email_status.sql` (cột status/type/error).

Admin quan sát: `GET /email/send/log`.

| Rule | |
|------|--|
| ✅ Log cả SUCCESS lẫn FAILED | |
| ❌ Không ghi plaintext OTP vào `description` / application log | OTP chỉ trong body mail |

---

## 6. Luồng gửi tổng quát

```
Caller (Auth OTP / QTHT / MKT / admin bulk)
  → EmailService.sendByTemplate / sendSimple / bulk
       → resolveActiveConfig + JavaMailSenderImpl
       → SMTP
       → logSendEmail → send_emails
```

---

## 7. Error codes (tóm tắt)

| Code | Khi nào |
|------|---------|
| `CONFIG_NOT_FOUND` | Không có SMTP active / host sai |
| `EMAIL_TEMPLATE_NOT_FOUND` | Sai `code` |
| `SEND_FAILED` | SMTP lỗi / không ai nhận được |
| `CONNECTION_FAILED` | Test connection fail |

Implement `ErrorCode` → `GlobalExceptionHandler`.

---

## 8. Consumer: OTP quên mật khẩu

OTP **không** gọi `EmailService` từ auth. Call chain:

```
AuthPasswordResetService.forgotPassword
  → NotificationService.notifyUserWithEmailFallback(..., urgent=true)
       → NotificationServiceImpl.trySendUrgentEmail
            ├─ sendByTemplate("URGENT_NOTIFICATION", {title, content}, [email])
            └─ catch → sendSimple(email, title, html)
```

| Hạng mục | Chi tiết |
|----------|----------|
| Plaintext OTP | Chỉ trong `message` / body mail |
| Template hiện dùng | `URGENT_NOTIFICATION` |
| Template sẵn chưa wire | `PASSWORD_RESET` |
| Auth vẫn success nếu mail fail | Anti-enumeration — admin xem `send_emails` |

Chi tiết E2E → [00-otp-overview.md](./00-otp-overview.md) · [module-server.md](./module-server.md) § Notification.

---

## 9. Checklist đọc code

- [ ] 5 controller: config / template / group / send / inbox
- [ ] `EmailServiceImpl.resolveActiveConfig` — max `updatedDate`
- [ ] `logSendEmail` SUCCESS + FAILED, `type=EMAIL`
- [ ] Seed runner `URGENT_NOTIFICATION` + `PASSWORD_RESET`
- [ ] OTP chỉ là consumer qua server Notification — không import auth
- [ ] `GET /email/send/log` cho vận hành

---

*Cập nhật khi đổi schema `send_emails`, template code, hoặc cách resolve SMTP config.*
