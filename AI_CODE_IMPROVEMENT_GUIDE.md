# Frezo Backend — AI Code Improvement Guide (INDEX)

> **File này là index.** Nội dung chuẩn đầy đủ nằm ở 4 file chuyên đề bên dưới.
> Đưa cho AI (Cursor / Claude / Copilot) trước khi code Java Spring Boot cho FrezoBE.

---

## 📚 Bộ tài liệu Backend Engineering

| # | File | Trọng tâm | Dòng | Đọc khi... |
|---|------|-----------|------|------------|
| 1 | **[AI_BACKEND_ENGINEERING_GUIDE.md](./AI_BACKEND_ENGINEERING_GUIDE.md)** ⭐ | Architecture, Package, Layer, Coding Rules, Performance, Security, Logging, Enterprise Rules | ~500 | **Trước MỌI task backend** |
| 2 | **[API_DESIGN_STANDARD.md](./API_DESIGN_STANDARD.md)** | REST verbs, `ApiResponse<T>`, error schema, pagination, filter, OpenAPI, idempotency, versioning | ~350 | Tạo/sửa Controller, đổi API contract |
| 3 | **[SPRING_BOOT_BEST_PRACTICE.md](./SPRING_BOOT_BEST_PRACTICE.md)** | DI, `@ConfigurationProperties`, JPA (LAZY/EntityGraph/Specification/Projection), Transaction, Validation, Exception, MapStruct, Lombok, AOP, Testing (Testcontainers), Actuator | ~500 | Viết Service, Repository, Config, Mapper, Test |
| 4 | **[DATABASE_STANDARD.md](./DATABASE_STANDARD.md)** | PostgreSQL naming, UUID, audit, soft delete, FK, index (composite, partial, GIN), Flyway migration, data types, query optimization, multi-tenancy | ~350 | Tạo/sửa Entity, migration script, tuning query |

Tổng: **~1700 dòng** chia 4 file → AI dễ tìm đúng quy tắc theo ngữ cảnh, không phải scan 1 file khổng lồ.

---

## 🎯 Nguyên tắc quan trọng nhất — Top 12

Nếu chỉ đọc 1 danh sách, đọc cái này:

1. **Controller chỉ orchestration** — không business, không mapping, không loop, không try-catch (đã có `GlobalExceptionHandler`)
2. **Service = business + transaction boundary** — `@Transactional`, gọi Repository, publish event, throw `AppException` với `ErrorCode` enum
3. **Repository chỉ query** — derived name / `@Query` JPQL / Specification / Projection. Không business, không if-else, không mapping
4. **1 wrapper duy nhất `ApiResponse<T>`** — bỏ hoàn toàn `com.frezo.util.web.Response` cũ. Không dùng `Map<String, Object>` làm response
5. **Pagination trả `PageResponse<T>`** — không `Map<String, Object>` với keys `"items"`, `"pageNumber"`, `"total"`
6. **1 exception duy nhất `AppException`** — bỏ `QTHTException`, `AuthException`. Kèm enum `ErrorCode` (`QthtErrorCode`, `CustomerErrorCode`, ...) map i18n key + HTTP status
7. **Constructor injection ONLY** — `@RequiredArgsConstructor` + `private final`. Không `@Autowired` field
8. **JPA `FetchType.LAZY` bắt buộc** cho mọi `@ManyToOne`/`@OneToOne`. EAGER = cấm. Load relation qua `@EntityGraph` per-query
9. **Pagination bắt buộc** mọi endpoint list. Không `pageSize = Integer.MAX_VALUE`. Whitelist sort field chống SQL injection
10. **Security bắt buộc:** BCrypt (KHÔNG `NoOpPasswordEncoder`), CORS whitelist env (KHÔNG `*`), JWT secret từ env (KHÔNG hardcode default), bật lại `@CheckPermission` + `@EnableMethodSecurity`
11. **Database Flyway** — tắt `ddl-auto: update`, mọi schema change qua `V<yyyyMMddHHmm>__desc.sql`. Không sửa migration đã apply
12. **Log: `log.error("msg", ex)`** — không `.getMessage()` (mất stack), không `System.out.println`, không log password/token/CCCD/OTP. Bắt buộc MDC `traceId`

---

## 🚫 5 anti-patterns hiện có trong codebase phải fix

Tất cả có trong `AI_BACKEND_ENGINEERING_GUIDE.md §13` (Migration Path). Ưu tiên theo mức độ rủi ro:

| # | Anti-pattern | File chuyên đề | Priority |
|---|--------------|----------------|----------|
| 1 | `NoOpPasswordEncoder` — password plain text trong DB | `AI_BACKEND_ENGINEERING_GUIDE.md §7.4` | **P0 — Security** |
| 2 | CORS `allowedOrigins("*")` + JWT secret hardcode default | `AI_BACKEND_ENGINEERING_GUIDE.md §7.1, §7.4` | **P0 — Security** |
| 3 | `@CheckPermission` toàn bộ controllers **bị comment-out** → RBAC không hoạt động | `AI_BACKEND_ENGINEERING_GUIDE.md §7.2, §7.4` | **P1 — Security** |
| 4 | Dual response wrapper: `ApiResponse` vs `Response` (util/web) | `API_DESIGN_STANDARD.md §2, §11` | **P1 — API contract** |
| 5 | Triple exception: `AppException` + `QTHTException` + `AuthException` | `SPRING_BOOT_BEST_PRACTICE.md §6.1` | **P1 — Code hygiene** |
| 6 | `Map<String, Object>` pagination response (unchecked cast) | `API_DESIGN_STANDARD.md §2.3, §4` | **P2 — Type safety** |
| 7 | `spring.jpa.hibernate.ddl-auto: update` + Flyway disabled | `DATABASE_STANDARD.md §6, §13` | **P2 — Data safety** |
| 8 | Không có `@Version` cho entity concurrent edit | `AI_BACKEND_ENGINEERING_GUIDE.md §9.3` | **P2 — Data safety** |
| 9 | `DriverManager.getConnection` trong `main()` để CREATE DATABASE | `SPRING_BOOT_BEST_PRACTICE.md §12` | **P3 — Code hygiene** |
| 10 | Không có `TraceIdFilter` / MDC → không debug được cross-service | `AI_BACKEND_ENGINEERING_GUIDE.md §8.1` | **P3 — Observability** |
| 11 | 0 test files (unit + integration) | `SPRING_BOOT_BEST_PRACTICE.md §10` | **P3 — Quality** |

---

## 🧭 Prompt template khi nhờ AI code

```
Task: <mô tả cụ thể>
Module: com.frezo.<domain>

Đọc:
- FrezoBE/AI_BACKEND_ENGINEERING_GUIDE.md
- FrezoBE/<file chuyên đề tương ứng>.md

Rule bắt buộc:
- Response: ApiResponse<T> hoặc PageResponse<T> (KHÔNG Response cũ, KHÔNG Map)
- Exception: AppException + <Domain>ErrorCode enum
- Validation: @Valid trong DTO
- Permission: @CheckPermission(api="...", action="...")
- Transaction: @Transactional (readOnly = true nếu query)
- FetchType: LAZY, N+1 fix bằng @EntityGraph hoặc DTO projection
- DI: constructor + @RequiredArgsConstructor + private final
- Test: JUnit 5 + Mockito (unit) hoặc Testcontainers (integration)

Sau khi code xong: chạy Self-Review Checklist ở AI_BACKEND_ENGINEERING_GUIDE.md §11
```

---

## 🔗 Quan hệ với chuẩn Frontend

Backend contract quyết định FE hiển thị. Xem đối chiếu:
- FE parse `ApiResponse<T>` (axios interceptor `packages/erp/src/lib/axios/axiosClient.ts`)
- FE parse `PageResponse<T>` cho grid
- FE hiển thị error message từ `messageCode` + `message` (đã có i18n)
- FE handle 401 → redirect login; 403 → toast "Không có quyền" (đã implement axios interceptor global)
- FE dùng `formatCurrencyVN`, `formatDateLong`, `formatPhoneVN` (đã có `packages/utils/src/format.ts`) → BE trả về số + ISO date, KHÔNG format sẵn

Reference: `FrezoFE/FE_UI_UX_STANDARD.md`.

---

*Đây là file INDEX. Sửa nội dung chuẩn ở 4 file chuyên đề, không nhồi vào file này.*
