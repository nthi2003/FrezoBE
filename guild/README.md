# Frezo Backend — Guild (Tech docs theo module)

> Tài liệu kỹ thuật **đọc code theo module** FrezoBE — cùng tone với [DATABASE_STANDARD.md](../DATABASE_STANDARD.md): mục số, bảng, call chain, rule ✅/❌, checklist.
> Mỗi file `module-*.md` ≈ một Maven module. Guide chuyên đề (OTP) nằm file `00-*.md`.

**guild ≠ product SRS.** Đây là tech map cho DEV/BA đọc code; chuẩn DB/naming vẫn ở `DATABASE_STANDARD.md`.

---

## 1. Cách mở Preview (Cursor / VS Code)

1. Mở file `.md` trong `guild/`.
2. `Ctrl + K` rồi `V` — **split**: source trái + preview phải (khuyến nghị, giống khi đọc `DATABASE_STANDARD.md`).
3. Hoặc `Ctrl + Shift + V` — Preview only.

---

## 2. Thứ tự đọc gợi ý

| Mục tiêu | Thứ tự |
|----------|--------|
| Onboard BE lần đầu | README → [module-common.md](./module-common.md) → [module-server.md](./module-server.md) → [module-auth-bom.md](./module-auth-bom.md) |
| OTP quên mật khẩu | [00-otp-overview.md](./00-otp-overview.md) → auth → common → server → email → qtht |
| Domain nghiệp vụ | Chọn nhóm dưới đây → mở đúng `module-*.md` |

---

## 3. Mục lục `module-*.md` theo domain

### 3.1 Nền tảng / Auth

| File | Module | Nội dung |
|------|--------|----------|
| [module-common.md](./module-common.md) | `module-common` | BaseEntity, exception, `@CheckPermission`, audit, rate limit, contracts |
| [module-server.md](./module-server.md) | `module-server` | Boot, ApiLog filter/aspect, NotificationServiceImpl, WS |
| [module-auth-bom.md](./module-auth-bom.md) | `module-auth-bom` | Login, 2FA, refresh, session/history, profile, OTP quên MK |

### 3.2 QTHT (Quản trị hệ thống)

| File | Module | Nội dung |
|------|--------|----------|
| [module-qtht-bom.md](./module-qtht-bom.md) | `module-qtht-bom` | (Guild hiện tập trung) khóa user + `ip_blacklist` khi OTP brute-force; module còn org/role/menu/API log… |

### 3.3 Email / thông báo kênh ngoài

| File | Module | Nội dung |
|------|--------|----------|
| [module-email-bom.md](./module-email-bom.md) | `module-email-bom` | SMTP config, template, inbox, bulk send, `send_emails` — OTP là một consumer |

### 3.4 Task / Event

| File | Module | Nội dung |
|------|--------|----------|
| [module-task-bom.md](./module-task-bom.md) | `module-task-bom` | Task, ticket, tag, ticket category |
| [module-event-bom.md](./module-event-bom.md) | `module-event-bom` | Sự kiện admin + portal RSVP |

### 3.5 Marketing / FB Automation

| File | Module | Nội dung |
|------|--------|----------|
| [module-fbautomation-bom.md](./module-fbautomation-bom.md) | `module-fbautomation-bom` | FB account/group/lead, automation, MKT posts/ads, public lead, affiliate |

### 3.6 Guide chuyên đề (không phải cả module)

| File | Phạm vi |
|------|---------|
| [00-otp-overview.md](./00-otp-overview.md) | Cross-module: OTP **quên mật khẩu** (subset auth + server + email + qtht) |

### 3.7 Module Maven chưa có guild riêng

Các BOM sau **chưa** có file trong `guild/` — đọc code trực tiếp / bổ sung sau:

| Domain | Module Maven |
|--------|--------------|
| HRM (QLNS) | `module-qlns-bom` |
| CRM | `module-crm-bom` |
| Customer / NCC | `module-customer-bom` |
| Warehouse | `module-warehouse-bom` |
| Accounting | `module-accounting-bom` |
| Product / Order | `module-product-bom` |
| Danh mục / Asset (DMDC) | `module-dmdc-bom` |
| QTBV / CMS | `module-qtbv-bom` |
| Approval | `module-approval-bom` |

Khi thêm guild mới: đặt tên `module-<maven-artifact>.md`, cập nhật bảng domain ở README này.

---

## 4. Rule viết guild

| Rule | Chi tiết |
|------|----------|
| ✅ Tiêu đề `# Frezo Backend — …` | Cùng tone standard docs |
| ✅ Blockquote `>` ngay dưới title | 1–2 câu mục đích + link liên quan |
| ✅ Mục số `## 1.` / `### 1.1` | Không emoji trang trí |
| ✅ Bảng class / API / constant | Dễ skim |
| ✅ Code fence call chain ngắn | Không paste cả class |
| ✅ / ❌ | Chỉ khi nêu rule bắt buộc / cấm |
| ✅ Checklist cuối file | Đọc code lần đầu |
| ❌ Prefix số trên tên `module-*.md` | Đặt theo artifact: `module-auth-bom.md` |
| ❌ Dump log raw / screenshot dài | Không thuộc guild kỹ thuật |

---

*Cập nhật README khi thêm/đổi file `module-*.md` hoặc đổi reading order.*
