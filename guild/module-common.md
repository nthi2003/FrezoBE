# Frezo Backend — module-common (Shared kernel)

> Thư viện **dùng chung** mọi BOM: `BaseEntity`, exception/API response, security annotation, audit, rate limit, notification/IP contracts, workflow/comment/OCR helpers.
> Impl IP block nằm `module-qtht-bom`; impl notification nằm `module-server`. OTP quên MK chỉ **dùng** một phần contracts (mục 9).
> Đọc cùng [README.md](./README.md) · [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) · [00-otp-overview.md](./00-otp-overview.md).

Package gốc: `com.frezo.common` (+ `com.frezo.util.web`).

---

## 1. Phạm vi module

| Nhóm | Package | Nội dung |
|------|---------|----------|
| Domain base | `domain` | `BaseEntity`, soft-delete helper |
| Exception | `exception` | `ErrorCode`, `AppException`, `GlobalExceptionHandler` |
| Response | `response` | `ApiResponse`, `PageResponse`, `FePage` |
| Security | `security` | `@CheckPermission`, `CryptoUtils` |
| Audit | `audit` | `AuditLogService`, aspect, entity audit |
| Rate limit | `ratelimit` | `RateLimitService` (Bucket4j + Caffeine) |
| Contracts | `service` | `NotificationService`, `IpBlockService`, MinIO, code sequence, AI doc |
| Constants | `constant` | `BlockReason`, `TimeBlock`, `WebSocketChannels` |
| Helper | `helper` | `SystemUtils`, `ServiceHelper`, `GenericSpecification` |
| Entity cross | `entity` | `Notification`, `AuditLog`, Comment*, CodeSequence, AI OCR |
| Workflow | `workflow` | Definition / instance / step / task |
| Web | `web` / `util.web` | Filter/helper HTTP |

**Không chứa:** SMTP entity, `ip_blacklist` entity, JWT filter (auth), business CRM/HRM.

---

## 2. `BaseEntity`

File: `com.frezo.common.domain.BaseEntity` — `@MappedSuperclass` + `AuditingEntityListener`.

| Field | Cột | Ghi chú |
|-------|-----|---------|
| `id` | `id` VARCHAR(36) | UUID string — `@PrePersist` nếu trống |
| `createdBy` | `created_by` | `@CreatedBy` |
| `createdDate` | `created_date` | `@CreatedDate` |
| `updatedBy` | `updated_by` | `@LastModifiedBy` |
| `updatedDate` | `updated_date` | `@LastModifiedDate` |
| `isDeleted` | `is_deleted` | Soft-delete flag |
| `deletedAt` | `deleted_at` | v1.1 |
| `deletedBy` | `deleted_by` | v1.1 |

```java
entity.softDelete(SystemUtils.getCurrentUsername());
repository.save(entity);
```

| Rule | |
|------|--|
| ✅ Mọi business entity kế thừa `BaseEntity` | Chuẩn [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) §3 |
| ❌ `@Version` trên BaseEntity | Opt-in từng entity concurrent-heavy (tránh MapStruct mất version) |
| ✅ Soft-delete đủ 3 field | Gọi `softDelete(username)` |

---

## 3. Exception & API response

### 3.1 `ErrorCode` + `AppException`

Mỗi module enum implement `ErrorCode` (`getKey`, `getStatus`, `getDefaultMessage`) — ví dụ `TaskErrorCode`, `EmailErrorCode`.

`AppException` mang `ErrorCode` (+ args message).  
`AuthException` — legacy `@Deprecated` (auth OTP vẫn có thể dùng).  
`QTHTException` — lỗi QTHT riêng nếu còn.

### 3.2 `GlobalExceptionHandler`

Map exception → `ApiResponse` + HTTP status từ `ErrorCode`.

### 3.3 Response wrappers

| Class | Dùng khi |
|-------|----------|
| `ApiResponse<T>` | Envelope chuẩn mọi API |
| `PageResponse<T>` | Phân trang backend |
| `FePage<T>` | Page shape FE (event portal, …) |
| `ComboboxResponse` | Dropdown |

---

## 4. Security — `@CheckPermission`

```java
@CheckPermission(api = "...", action = "VIEW")  // CREATE / UPDATE / DELETE / ...
```

| Hạng mục | Chi tiết |
|----------|----------|
| Annotation | `module-common` — domain compile không phụ thuộc qtht |
| Aspect enforce | Sống ở `module-qtht-bom` (permission theo menu/path) |
| Crypto | `CryptoUtils` — mã hóa field nhạy cảm (phone, …) |

---

## 5. Audit

| Class | Vai trò |
|-------|---------|
| `AuditLogService` | `logAction(action, entityType, entityId, summary, request)` |
| `AuditLogAspect` / `AuditLogAudit` | AOP / entity nếu dùng |
| `AuditLogController` | API xem audit (common controller) |
| `AuditAction` | Hằng / enum action |

OTP brute-force gọi: `logAction("OTP_BRUTE_FORCE_LOCK", "users", …)`.

---

## 6. Rate limit — `RateLimitService`

Bucket4j + Caffeine in-memory:

| Method | Limit (mặc định code) |
|--------|------------------------|
| `tryConsumeByIp(ip)` | 100 req / phút |
| `tryConsumeByUser(userId, isAdmin)` | 100 / phút (admin 200) |
| `tryLoginAttempt(username)` | 5 fail / 15 phút |
| `resetLoginAttempts(username)` | Clear sau login OK |

| Rule | |
|------|--|
| ✅ Dùng kèm `IpBlockService` (DB) | Rate limit = tầng nhanh; blacklist = tầng bền |
| ⚠️ Multi-instance | Cache local — không share giữa node |

Public lead có rate limiter riêng trong fbautomation (`PublicLeadRateLimiter`).

---

## 7. Contracts quan trọng

### 7.1 `NotificationService`

| Method | Mục đích |
|--------|----------|
| `notifyUserWithEmailFallback` | Legacy urgent (OTP) |
| `notify` / `notifyMany` | Domain event + deep-link |
| `sendToUser` / `sendToTopic` | WS thuần |
| Inbox APIs | get / mark read / counts |

Impl: [module-server.md](./module-server.md) `NotificationServiceImpl`.

### 7.2 `IpBlockService`

```java
void checkIpBlocked(String ipAddress, String userName);
void handleFailedAttempt(String ipAddress, String targetUserName, BlockReason reason);
void clearFailedAttempts(String ipAddress, String userName);
void lockUserAndBlacklistIp(String ip, String userName, BlockReason reason, Integer banMinutes);
```

Impl: `IpBlockServiceImpl` ([module-qtht-bom.md](./module-qtht-bom.md)).  
`banMinutes = null` → ban vô thời hạn trên `ip_blacklist`.

### 7.3 `BlockReason`

| Value | Nghĩa |
|-------|-------|
| `BRUTE_FORCE` | Sai password nhiều lần |
| `WRONG_PASSWORD` | Sai MK |
| `OTP_BRUTE_FORCE` | Sai OTP quên MK ≥ 5 lần |

`TimeBlock` — LEVEL_1…LEVEL_MAX (phút khóa tạm trên `block_ip`).

### 7.4 Service khác

| Interface / class | Vai trò |
|-------------------|---------|
| `MinioService` | Upload file / avatar |
| `CodeSequenceService` | Sinh mã (TICKET-0001, …) |
| `AiDocumentService` | OCR / AI doc |
| `AuditLogQueryService` | Query audit |

---

## 8. Entity & feature cross-cutting khác

| Entity / area | Bảng / ý nghĩa |
|---------------|----------------|
| `Notification` | `notifications` — bell |
| `AuditLog` | Audit trail |
| Comment* | Comment + attachment + mention |
| `CodeSequence` | Sequence mã nghiệp vụ |
| `AiOcrDocumentRecord` | OCR |
| Workflow* | Định nghĩa / instance phê duyệt nhẹ trong common |

Controllers trong common: `AuditLogController`, `AiDocController`.

---

## 9. OTP quên mật khẩu — contracts dùng đến

```
AuthPasswordResetService
  ├─ NotificationService.notifyUserWithEmailFallback  → server
  ├─ IpBlockService.lockUserAndBlacklistIp            → qtht
  ├─ BlockReason.OTP_BRUTE_FORCE
  ├─ AuditLogService.logAction("OTP_BRUTE_FORCE_LOCK")
  └─ AuthException (legacy) / AppException
```

| Rule | |
|------|--|
| ✅ Auth phụ thuộc interface common | Không email/qtht impl |
| ❌ Đặt SMTP / `IpBlacklist` entity trong common | |

Guide chuyên sâu: [00-otp-overview.md](./00-otp-overview.md).

---

## 10. Helper thường dùng

| Class | Việc |
|-------|------|
| `SystemUtils.getCurrentUsername()` | User JWT hiện tại |
| `ServiceHelper.createPageable` | Page 1-based |
| `GenericSpecification` | Filter JPA động |

---

## 11. Checklist đọc code

- [ ] `BaseEntity` + `softDelete`
- [ ] `ErrorCode` / `AppException` / `GlobalExceptionHandler`
- [ ] `@CheckPermission` (annotation common, aspect qtht)
- [ ] `RateLimitService` limits
- [ ] `NotificationService` + `IpBlockService` + `BlockReason`
- [ ] `AuditLogService`
- [ ] OTP chỉ đụng subset contracts — không phải toàn bộ common

---

*Cập nhật khi đổi BaseEntity audit fields, ErrorCode contract, hoặc signature IpBlock/Notification.*
