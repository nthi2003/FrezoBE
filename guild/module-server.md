# Frezo Backend — module-server (Bootstrap & cross-cutting)

> Module **host Spring Boot**: assemble toàn bộ BOM, WebSocket, notification impl, API log filter/aspect, cache/Swagger.
> Không chỉ là cầu OTP — OTP dùng nhánh urgent email trong `NotificationServiceImpl` (mục 5).
> Đọc cùng [README.md](./README.md) · [module-common.md](./module-common.md) · [module-email-bom.md](./module-email-bom.md) · [module-auth-bom.md](./module-auth-bom.md) · [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

Package gốc: `com.frezo.server`. Entry: `FrezoServerApplication`.

---

## 1. Vai trò trong hệ thống

| Vai trò | Chi tiết |
|---------|----------|
| Boot host | `@SpringBootApplication` — scan / import mọi `module-*-bom` |
| Wiring DI | Impl các interface common (`NotificationService`, …) nằm đây hoặc BOM được assemble |
| HTTP cross-cut | `ApiLogFilter` (chính) + `ApiLogAspect` (legacy, tắt mặc định) |
| Realtime | `WebSocketConfig` + `NotificationServiceImpl` push STOMP |
| Ops | `SystemController`, `DashboardServiceImpl`, Swagger, cache |

**Rule kiến trúc:**

| Rule | |
|------|--|
| ✅ Domain BOM không phụ thuộc server | Auth/email/qtht là library; server assemble |
| ✅ Interface ở common, impl ở server (hoặc BOM được server load) | Tránh circular |
| ❌ Đặt business entity mới chỉ trong server | Đưa vào đúng BOM |

---

## 2. Class map

| Class | Package | Vai trò |
|-------|---------|---------|
| `FrezoServerApplication` | `com.frezo.server` | Boot entry |
| `NotificationServiceImpl` | `service` | Impl `NotificationService` — DB + WS + email urgent + push |
| `DashboardServiceImpl` | `service` | Aggregate dashboard |
| `SystemController` | `controller` | System endpoints |
| `ApiLogFilter` | `component` | ★ Ghi API log mọi request (trừ noise) |
| `ApiLogWriter` | `component` | Persist async/sync `ApiLog` (qtht) |
| `CachedBodyHttpServletRequest` | `component` | Đọc lại body để log |
| `ApiLogAspect` | `aspect` | Legacy AOP — `frezo.api-log.aspect-enabled=true` mới bật |
| `ApiLogProperties` | `config` | Prefix `frezo.api-log` |
| `WebSocketConfig` | `config` | STOMP endpoint |
| `SwaggerConfig` | `config` | OpenAPI |
| `ServerCacheConfig` | `config` | Cache |
| `MapStructConfig` | `config` | Mapper |

---

## 3. Bootstrap

```
FrezoServerApplication
  → Spring Boot auto-config
  → Component scan com.frezo.*
  → Security (auth module) + filters (ApiLogFilter Order=1)
  → Flyway migrations (thường resources trong module-server)
  → WebSocket + Swagger
```

Migration SQL nằm dưới `module-server/src/main/resources/db/migration/` (ví dụ `V202608061000__send_email_status.sql`).

---

## 4. API log

### 4.1 `ApiLogFilter` (mặc định dùng)

| Hạng mục | Chi tiết |
|----------|----------|
| Order | `@Order(1)` — `OncePerRequestFilter` |
| Persist | Qua `ApiLogWriter` → `ApiLog` / `ApiLogService` (qtht) |
| Username | Lấy **sau** `filterChain` (JWT đã authenticate) |
| Body | Mask field nhạy cảm: password, token, secret, apiKey, … |
| Skip | Noise path (health, swagger, static — theo `ApiLogProperties`) |
| IP gate | Có thể check `IpBlacklistService` / `IpWhitelistService` |

### 4.2 `ApiLogAspect` (legacy)

- Pointcut: `execution(* com.frezo.*.controller..*(..))`
- **Tắt mặc định:** `frezo.api-log.aspect-enabled=false`
- Chỉ bật khi cần so sánh / rollback khẩn với Filter

| Rule | |
|------|--|
| ✅ Dùng Filter làm nguồn chính | Tránh double-log |
| ❌ Bật aspect + filter cùng lúc lâu dài | Trùng bản ghi |

Entity/API admin xem log: `module-qtht-bom` — `ApiLogController` `/qtht/...`.

---

## 5. Notification service

Interface: `com.frezo.common.service.NotificationService`.  
Impl duy nhất runtime: `NotificationServiceImpl`.

### 5.1 Khả năng

| Method nhóm | Hành vi |
|-------------|---------|
| `notify` / `notifyMany` | DB `notifications` + WS + email nếu `urgent` |
| `notifyUserWithEmailFallback` | Legacy — OTP / urgent đơn giản |
| `sendToUser` / `sendToTopic` | Push STOMP |
| `getMyNotifications` / mark read / counts | Inbox bell |

Dependencies inject:

| Bean | Mục đích |
|------|----------|
| `NotificationRepository` | Persist |
| `SimpMessagingTemplate` | WS |
| `UserRepository` | Lookup email theo username |
| `EmailService` | SMTP template/simple |
| `ObjectProvider<PushNotificationService>` | Mobile push (optional) |

### 5.2 Nhánh OTP (urgent email)

```
AuthPasswordResetService
  → notifyUserWithEmailFallback(username, title, message, urgent=true)
       → notify(..., type=WARNING, urgent=true)
            ├─ save Notification (priority=URGENT)
            ├─ sendToUser(... /queue/notifications)
            └─ trySendUrgentEmail
                 ├─ sendByTemplate("URGENT_NOTIFICATION", {title, content}, [email])
                 └─ catch → sendSimple(...)
```

| Rule | |
|------|--|
| ✅ Auth chỉ gọi interface common | |
| ✅ Server mới gọi `EmailService` | |
| ❌ Auth inject `EmailService` | |

Chi tiết OTP E2E → [00-otp-overview.md](./00-otp-overview.md).

### 5.3 WebSocket

`WebSocketConfig` — FE subscribe queue user (vd `/queue/notifications`). Channels constants: `WebSocketChannels` (common).

---

## 6. System & dashboard

| Class | Ghi chú |
|-------|---------|
| `SystemController` | Health / system info (tuỳ path) |
| `DashboardServiceImpl` | Aggregate số liệu dashboard (gọi nhiều BOM) |

---

## 7. Config properties liên quan

| Prefix / key | Ý nghĩa |
|--------------|---------|
| `frezo.api-log.*` | Bật filter behavior, async, aspect-enabled |
| `frezo.inbox.notify-users` | (fbautomation) user nhận bell lead public |
| Context path | Thường `/api` trong `application.yml` |

---

## 8. Checklist đọc code

- [ ] `FrezoServerApplication` + Flyway dưới `resources/db/migration`
- [ ] `ApiLogFilter` vs `ApiLogAspect` — cái nào đang bật
- [ ] `NotificationServiceImpl` — notify / urgent email / WS
- [ ] `WebSocketConfig`
- [ ] OTP: chỉ đọc nhánh `trySendUrgentEmail` — không phải toàn bộ module
- [ ] Cross: [module-email-bom.md](./module-email-bom.md), [module-common.md](./module-common.md), [module-qtht-bom.md](./module-qtht-bom.md) (ApiLog entity)

---

*Cập nhật khi đổi filter API log, WebSocket path, hoặc impl Notification.*
