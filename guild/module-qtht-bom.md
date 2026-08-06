# Frezo Backend — module-qtht-bom (Quản trị hệ thống)

> Module **QTHT**: tổ chức / phòng ban / nhân sự, quản trị user–role–menu–permission, cấu hình, bảo mật IP, API log, analytics, job nền, dashboard, thông báo, comment, workflow facade, Docs Hub (Guide).
> Đọc **cùng** [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) · [module-auth-bom.md](./module-auth-bom.md) · [module-common.md](./module-common.md) · [00-otp-overview.md](./00-otp-overview.md) (mục khóa OTP).

Package gốc: `com.frezo.qtht`. Module Maven: `module-qtht-bom`. Context HTTP thường `/api` (ví dụ `/api/qtht/organization`).

**Phụ thuộc chính:** `module-auth-bom` (User/UserRole), `module-common` (Notification, Comment, Workflow contracts, `IpBlockService` interface), `module-email-bom` (báo cáo tuần).

---

## 1. Phạm vi module

| Trong `module-qtht-bom` | Ngoài module (chỉ gọi / dùng) |
|-------------------------|--------------------------------|
| Organization, Department, Person, PersonDocument | `User` / login / JWT / forgot-password → `module-auth-bom` |
| Role, Menu, Permission, RoleMenu, RolePermission, MenuPermission | Seed SQL menu/role/permission → `module-auth-bom` `DataInitializer` |
| Setting, IP blacklist/whitelist/trust, BlockIP | Interface `IpBlockService`, `BlockReason` → `module-common` |
| ApiLog, SystemJob, ErpPageView, Guide, UserDevice | Flyway schema → `module-server` |
| Controllers: Notification, Comment, Workflow (facade) | Entity/service Notification, Comment, Workflow engine → `module-common` |
| Jobs: `DB_BACKUP`, `WEEKLY_REPORT` + scheduler động | Job code khác (stock/email/…) do module khác đăng ký bean |

**Không có** controller CMS website public trong QTHT. Website public → `module-qtbv-bom` (`/public/*`). Guide (`/qtht/guides`) = Docs Hub **nội bộ**.

---

## 2. Class map (controllers theo domain)

| Domain | Controller | Base path |
|--------|------------|-----------|
| Tổ chức | `OrganizationController` | `/qtht/organization` |
| Phòng ban | `DepartmentController` | `/qtht/department` |
| Nhân sự | `PersonController` | `/qlns/person` |
| Tài liệu NS | `PersonDocumentController` | `/qtht/person-document` |
| User admin | `UserAdminController` | `/qtht/user` |
| Device push | `UserDeviceController` | `/qtht/user-device` |
| Role | `RoleController` | `/qtht/role` |
| Menu | `MenuController` | `/qtht/menu` |
| Permission | `PermissionController` | `/qtht/permission` |
| Role–Menu | `RoleMenuController` | `/qtht/role-menu` |
| Setting | `SettingController` | `/qtht/setting` |
| IP blacklist | `IpBlacklistController` | `/qtht/ip-blacklist` |
| IP whitelist | `IpWhitelistController` | `/qtht/ip-whitelist` |
| IP trust | `IPTrustController` | `/qtht/ip-trust` |
| Gateway nội bộ | `InternalGatewayController` | `/qtht/internal-gateway` |
| API log | `ApiLogController` | `/qtht/api-log` |
| Jobs | `SystemJobController` | `/qtht/jobs` |
| Backup tay | `SystemController` | `/qtht/system` |
| Dashboard | `DashboardController` | `/qtht/dashboard` |
| Usage | `UsageAnalyticsController` | `/qtht/usage` |
| Thông báo | `NotificationController` | `/qtht/notification` |
| Comment | `CommentController` | `/comments` |
| Workflow | `WorkflowController` | `/wf` |
| Visual WF | `VisualWorkflowController` | `/workflows` |
| Guide CMS | `GuideController` | `/qtht/guides` |
| WebSocket | `WebSocketChannelController`, `TestWebSocketController` | `/qtht/websocket-channel`, `/qtht/test-ws` |

Quy ước permission: hầu hết endpoint gắn `@CheckPermission(api, action)`. Combobox / một số bootstrap endpoint **chỉ cần JWT** (không gắn annotation) — cố ý tránh deadlock UI.

---

## 3. Tổ chức & phòng ban

### 3.1 Organization — `/qtht/organization`

| Method | Path | Action | Mục đích |
|--------|------|--------|----------|
| GET | `/` | VIEW | List + filter + phân trang |
| POST | `/` | CREATE | Tạo tổ chức |
| PUT | `/{id}` | UPDATE | Cập nhật |
| DELETE | `/{id}` | DELETE | Soft-delete |
| GET | `/combobox` | (JWT) | Lookup combobox |

Entity `Organization` → bảng `organization`: `code`, `taxCode`, `name`, `website`, `email`, `phone`, `address`, `parentId`, `level`, `path`, `type`, `scale`, `status`, `legalRepresentativeId` (FK `Person`).

### 3.2 Department — `/qtht/department`

| Method | Path | Action | Mục đích |
|--------|------|--------|----------|
| GET | `/` | VIEW | List + filter |
| GET | `/tree` | VIEW | Cây phòng ban |
| GET | `/combobox` | (JWT) | Combobox |
| POST | `/` | CREATE | Tạo |
| PUT | `/{id}` | UPDATE | Sửa |
| PUT | `/{id}/activate` | UPDATE | Kích hoạt |
| PUT | `/{id}/deactivate` | UPDATE | Vô hiệu |
| DELETE | `/{id}` | DELETE | Soft-delete |

Entity `Department` → `department`: `code`, `name`, `organizationId`, `parentId`, `level`, `path`, `status`, `managerId`, `deputyManagerId`.

`DepartmentHistory` hiện là POJO — **chưa** map bảng production.

---

## 4. Nhân sự (Person)

### 4.1 Person — `/qlns/person`

| Method | Path | Action | Mục đích |
|--------|------|--------|----------|
| GET | `/all` | VIEW | Danh sách |
| GET | `/{id}` | VIEW | Chi tiết |
| POST | `/` | CREATE | Tạo person |
| PUT | `/{id}` | UPDATE | Cập nhật |
| PUT | `/{id}/activate` / `/deactivate` | UPDATE | Bật/tắt |
| DELETE | `/{id}` | DELETE | Xóa |
| GET | `/combobox` | (JWT) | Combobox (`value` = username cho CRM) |
| POST | `/upload-avatar-temp` | UPDATE | Upload avatar tạm MinIO |

Entity `Person` → `person`: `code`, `name`, `activated`, `gender`, `dob`, `email`, `phone`, `jobTitle`, **`isAdmin`**, `avatarUrl`, `departmentId`, `orgId`.

**Rule:** User auth gắn `personId`. `Person.isAdmin = true` (hoặc role `ADMIN`) → bypass full menu / permission check ở một số nhánh.

### 4.2 PersonDocument — `/qtht/person-document`

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/{personId}` | List tài liệu |
| POST | `/{personId}/upload` | Upload CV/cert |
| DELETE | `/{personId}/{documentId}` | Xóa |

Bảng `person_document`: `personId`, `type` (CV/CERT/…), `fileName`, `fileUrl`, `fileSize`.

---

## 5. User / Role / Menu / Permission

### 5.1 Phân chia với `module-auth-bom`

| Khía cạnh | `module-auth-bom` | `module-qtht-bom` |
|-----------|-------------------|-------------------|
| Entity | `User`, `UserRole`, session, login history | `Person`, `Role`, `Menu`, `Permission`, `RoleMenu`, … |
| Runtime auth | Login, JWT, OTP 2FA, forgot/reset password | Không xử lý login |
| Quản trị user UI | — | `UserAdminController` |
| Catalog RBAC | Seed SQL boot | CRUD API + sidebar + `checkPermission` |
| Menu FE bootstrap | — | `GET /qtht/menu/user/{username}` |

### 5.2 UserAdmin — `/qtht/user`

| Method | Path | Action | Mục đích |
|--------|------|--------|----------|
| GET | `/search` | VIEW | Autocomplete @mention |
| GET | `/all` | VIEW | List phân trang |
| GET | `/{id}` | VIEW | Chi tiết |
| GET | `/{username}/roles` | VIEW | Role của user |
| POST | `/register` | CREATE | Tạo User + Person + role |
| POST | `/assign-role` | CREATE | Gán role |
| PUT | `/{id}` | UPDATE | Sửa user |
| PUT | `/{id}/active` | UPDATE | Mở khóa tài khoản (`status=1`) |
| PUT | `/{id}/lock` | UPDATE | Khóa tài khoản (`status=0`) |
| POST | `/{id}/reset-password` | UPDATE | Reset MK mặc định |

### 5.3 Role — `/qtht/role`

CRUD theo `appCode`: tạo / sửa / soft-delete / list / combobox. Bảng `roles`: unique `(app_code, code)`.

### 5.4 Menu — `/qtht/menu`

| Method | Path | Ghi chú |
|--------|------|---------|
| GET | `/user/{username}` | Sidebar FE — **không** `@CheckPermission` (tránh deadlock) |
| GET | `/`, `/{id}` | Admin list/detail |
| POST / PUT / DELETE | … | CRUD admin |

Bảng `menu`: `code`, `appCode`, `name`, `parentCode`, `orderIndex`, `menuType`, `isPublic`, `icon`, `feUrl`, `folderPath`, `status`.

### 5.5 Permission & RoleMenu

| Controller | Path chính | Việc |
|------------|------------|------|
| `PermissionController` | `/qtht/permission` | Catalog permission endpoint |
| `RoleMenuController` | `/qtht/role-menu` | `GET /role/{roleCode}`, `POST /save-all` (ghi đè) |

Bảng join: `permission`, `role_menu`, `role_permission`, `menu_permission`.

Lõi check runtime: `PermissionRepository.checkPermission()` (native) — aspect `@CheckPermission`.

### 5.6 UserDevice — `/qtht/user-device`

| Method | Path | Mục đích |
|--------|------|----------|
| POST | `/register` | Đăng ký Expo push token |
| POST | `/unregister` | Hủy device |

Bảng `user_device`: `username`, `expoPushToken` (unique), `platform`, `deviceName`, `deviceId`, `isActive`.

---

## 6. Cấu hình hệ thống (Setting)

Controller: `SettingController` — `/qtht/setting`.

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/` | Tất cả setting |
| GET | `/org/{orgId}` | Theo tổ chức |
| POST | `/` | Tạo |
| PUT | `/{id}` | Sửa |

Entity `Setting` → `setting`: `orgId`, flags `isEmail` / `isSwap` / `isColor` / `isAttendance`, JSON `details`, cấu hình ca, `maxMembers`, require avatar/CV/health cert, duyệt bài, …

`isColor` = bật/tắt theme màu website ERP (không phải public CMS).

---

## 7. Bảo mật IP & khóa OTP brute-force

> Một mục trong QTHT — chi tiết OTP end-to-end xem [00-otp-overview.md](./00-otp-overview.md) và [module-auth-bom.md](./module-auth-bom.md).

### 7.1 Bốn lớp IP

| Lớp | Bảng | Mục đích | API |
|-----|------|----------|-----|
| `BlockIP` | `block_ip` | Progressive fail OTP/login theo IP+user | Không REST — qua `IpBlockServiceImpl` |
| `IpBlacklist` | `ip_blacklist` | Ban có hạn / vô hạn; Gateway rate-limit | `/qtht/ip-blacklist`, `/qtht/internal-gateway` |
| `IpWhitelist` | `ip_whitelist` | Allow-list khi bật whitelist | `/qtht/ip-whitelist` |
| `IPTrust` | `ip_trust` | IP tin cậy đặt tên riêng | `/qtht/ip-trust` |

### 7.2 OTP sai 5 lần — call chain

Call site: `AuthPasswordResetService.lockAfterBruteForce` (`module-auth-bom`).

```
AuthPasswordResetService.lockAfterBruteForce
  ├─ clear reset_key / reset_date
  ├─ ipBlockService.handleFailedAttempt(ip, userName, OTP_BRUTE_FORCE)  → block_ip
  ├─ ipBlockService.lockUserAndBlacklistIp(ip, userName, OTP_BRUTE_FORCE, null)
  │     ├─ users.status = 0
  │     └─ ipBlacklistService.addBanMinutes(..., "SYSTEM", null)  // vô hạn
  └─ auditLogService.logAction("OTP_BRUTE_FORCE_LOCK", "users", …)
```

| Hạng mục | Giá trị |
|----------|---------|
| Interface | `IpBlockService` (`module-common`) |
| Impl | `IpBlockServiceImpl` (`module-qtht-bom`) |
| `banMinutes` OTP | `null` → `banned_until = null` (vô hạn tới admin unban) |
| `bannedBy` | `"SYSTEM"` |
| Note | `"OTP_BRUTE_FORCE — user {userName}"` |
| Audit action | `OTP_BRUTE_FORCE_LOCK` |

### 7.3 Threshold `block_ip` (`IpBlockServiceImpl`)

| Constant | Value | Ý nghĩa |
|----------|-------|---------|
| `THRESHOLD_BLOCK_START` | `6` | Fail ≥ 6 trên cùng IP+user → khóa tạm `TimeBlock` |
| `THRESHOLD_LOCK_ACCOUNT` | `11` | Tổng fail ≥ 11 (1 ngày) → khóa account + `LEVEL_MAX` |

`TimeBlock`: LEVEL_1=5p … LEVEL_4=60p, LEVEL_MAX=24h (hoặc +10 năm khi lock account).

**Lưu ý OTP:** Auth khóa sau **5** lần qua `lockUserAndBlacklistIp`; `handleFailedAttempt` vẫn ghi `block_ip` cho thống kê / gate sau.

### 7.4 Admin API blacklist

| Method | Path | Action |
|--------|------|--------|
| POST | `/qtht/ip-blacklist/ban` | CREATE — `ipAddress`, `reason`, `bannedBy?`, `hours?` |
| DELETE | `/qtht/ip-blacklist/unban/{id}` | DELETE |
| GET | `/qtht/ip-blacklist` | VIEW — ban đang hiệu lực |

Entity `IpBlacklist`: `ipAddress`, `reason`, `bannedBy`, `bannedUntil`, `active` (+ `BaseEntity`).

### 7.5 Whitelist / Trust / Internal Gateway

| API | Mục đích |
|-----|----------|
| `/qtht/ip-whitelist` | CRUD allow-list + `GET /check` |
| `/qtht/ip-trust` | CRUD IP tin cậy (`ipNumber`, `ipName`, `isTrust`) |
| `/qtht/internal-gateway` | `GET /blacklist`, `GET /whitelist`, `POST /block-ip` — **không** `@CheckPermission` (nội bộ Gateway) |

**Rule:**
- ✅ Khóa OTP + blacklist qua `IpBlockService`, không set `status` ad-hoc ngoài luồng
- ✅ Admin mở lại: `PUT /qtht/user/{id}/active` + `DELETE .../unban/{id}`
- ❌ Không nhầm `/auth/verify-otp` (2FA) với `/auth/verify-reset-otp`

---

## 8. API Log (audit HTTP)

Controller: `ApiLogController` — `/qtht/api-log`.

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/` | List + filter |
| GET | `/stats` | Thống kê |
| GET | `/{id}` | Chi tiết |
| DELETE | `/{id}` | Xóa 1 |
| DELETE | `/bulk/{days}` | Xóa log cũ hơn N ngày |

Entity `ApiLog` → `api_log`: `uri`, `method`, `ipAddress`, `username`, `statusCode`, `duration`, `requestBody`, `responseBody`, `userAgent`, `queryString`, `module`, `errorMessage`, `traceId`, `effFrom` / `effTo`.

Migration liên quan: `module-server/.../V202608051700__api_log_audit_columns.sql`.

Ghi log thường qua interceptor/filter (server) → `ApiLogService`.

---

## 9. Usage analytics

Controller: `UsageAnalyticsController` — `/qtht/usage`.

| Method | Path | Permission | Mục đích |
|--------|------|------------|----------|
| POST | `/pageview` | (JWT, không CheckPermission) | FE ghi pageview |
| GET | `/pageviews/top?days` | VIEW | Top route/module (1–30 ngày) |

Entity `ErpPageView` → `erp_page_view`: `username`, `route`, `moduleCode`, `viewedAt`.

---

## 10. System jobs & backup

### 10.1 Catalog & API — `/qtht/jobs`

| Method | Path | Action | Mục đích |
|--------|------|--------|----------|
| GET | `/` | VIEW | Danh sách job |
| PUT | `/{code}` | UPDATE | Đổi cron / bật tắt |
| POST | `/{code}/run` | EXECUTE | Chạy tay |
| GET | `/{code}/history` | VIEW | Lịch sử |
| GET | `/preview-cron` | VIEW | Preview mốc cron |

Bảng: `system_job`, `system_job_history`.

### 10.2 Job thuộc QTHT

| `job_code` | Class | Cron mặc định | Việc |
|------------|-------|---------------|------|
| `DB_BACKUP` | `DatabaseBackupJob` | `0 0 12 * * *` | `pg_dump` → Google Drive (`GoogleDriveService`) |
| `WEEKLY_REPORT` | `WeeklyReportJob` | `0 0 8 * * MON` | Email báo cáo tuần (`ReportService`) |

`DynamicJobScheduler`: load bean `SchedulableJob`, sync row DB, reschedule khi admin đổi cron, anti-overlap (SKIPPED nếu đang chạy). Pool: `SystemJobSchedulerConfig` → `systemJobTaskScheduler` (4 threads).

Seed Flyway: `V202608051610__system_job.sql` (có thêm job code module khác khi bean được load).

### 10.3 Backup thủ công

`POST /qtht/system/backup` — `SystemController`, action `EXECUTE` — backup ngoài lịch scheduler.

---

## 11. Dashboard & báo cáo

| Endpoint | Controller | Mục đích |
|----------|------------|----------|
| `GET /qtht/dashboard/summary` | `DashboardController` | Tổng hợp dashboard |
| `GET /qtht/dashboard/export/attendance` | `DashboardController` | Export Excel chấm công tháng (`EXPORT`) |

Interface `DashboardService` ở qtht; **impl aggregate** thường ở `module-server` (`DashboardServiceImpl`).

`ReportService`: export attendance (có thể stub) + `sendWeeklyReport()` cho job tuần.

---

## 12. Thông báo

Controller: `NotificationController` — `/qtht/notification` (facade → `NotificationService` common).

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/my` | Hộp thư của tôi |
| GET | `/unread-count` | Badge chưa đọc |
| PATCH | `/{id}/read` | Đánh dấu đọc |
| PATCH/POST | `/mark-all-read` | Đọc hết |

Push mobile: `PushNotificationService` + `UserDevice` (Expo).

Gửi email OTP **không** đi qua controller này — xem [module-server.md](./module-server.md) / [module-email-bom.md](./module-email-bom.md).

---

## 13. Comment & @mention

Controller: `CommentController` — `/comments`.

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/?subjectType&subjectId` | Thread theo subject |
| POST | `/` | Tạo + @mention |
| POST | `/attachments` | Upload MinIO |
| PUT | `/{id}` | Sửa (author) |
| DELETE | `/{id}` | Soft-delete |

Entity/service nằm `module-common`; controller QTHT = HTTP surface dùng chung nhiều module.

---

## 14. Workflow

| Controller | Base | Việc |
|------------|------|------|
| `WorkflowController` | `/wf` | Definitions CRUD, instances theo entity, cancel, tasks mine / approve / reject |
| `VisualWorkflowController` | `/workflows` | Template, clone, validate graph, đọc/ghi graph designer |

Engine & entity workflow: `module-common`. QTHT chỉ expose REST facade + permission.

---

## 15. Guide CMS (Docs Hub) — không phải website public

Controller: `GuideController` — `/qtht/guides`.

| Method | Path | Mục đích |
|--------|------|----------|
| GET | `/published`, `/published/{slug}` | Hub docs đã publish |
| GET | `/`, `/{id}` | Admin list/detail |
| POST / PUT / DELETE | … | CRUD |
| PUT | `/{id}/publish` / `unpublish` | Xuất / gỡ bản |

Entity `Guide` → `guide`: `slug` (unique), `title`, `body`, `module`, `summary`, `sortOrder`, `published`.

Seed boot: `GuideDataInitializer` + `src/main/resources/guides/*.md` (~35 bài). Migration: `V202607241040__guide_cms_fr_doc_03.sql`.

**Website public** (landing, banner, bài PUBLIC) → [module-qtbv-bom.md](./module-qtbv-bom.md).

---

## 16. WebSocket (hỗ trợ)

| Controller | Path | Mục đích |
|------------|------|----------|
| `WebSocketChannelController` | `/qtht/websocket-channel` | Liệt kê channel |
| `TestWebSocketController` | `/qtht/test-ws` | Test push topic / user |

---

## 17. Bản đồ entity / bảng

Audit chung (`BaseEntity`): `id` UUID 36, `createdBy/Date`, `updatedBy/Date`, `isDeleted`, `deletedAt`, `deletedBy` — theo [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

| Entity | Bảng | Ghi chú |
|--------|------|---------|
| `Organization` | `organization` | Cây org + đại diện PL |
| `Department` | `department` | Cây PB theo org |
| `Person` | `person` | Nhân sự; `isAdmin` |
| `PersonDocument` | `person_document` | File NS |
| `Role` | `roles` | Unique `(app_code, code)` |
| `Menu` | `menu` | Sidebar FE |
| `Permission` | `permission` | Catalog API |
| `RoleMenu` | `role_menu` | Gán menu |
| `RolePermission` | `role_permission` | Gán permission |
| `MenuPermission` | `menu_permission` | Menu↔permission |
| `Setting` | `setting` | Cấu hình org/hệ |
| `BlockIP` | `block_ip` | **Không** `BaseEntity` |
| `IpBlacklist` | `ip_blacklist` | Ban IP |
| `IpWhitelist` | `ip_whitelist` | Allow-list |
| `IPTrust` | `ip_trust` | IP tin cậy |
| `ApiLog` | `api_log` | HTTP audit |
| `SystemJob` | `system_job` | Cron động |
| `SystemJobHistory` | `system_job_history` | Lịch sử chạy |
| `ErpPageView` | `erp_page_view` | Analytics FE |
| `Guide` | `guide` | Docs Hub |
| `UserDevice` | `user_device` | Expo push |

Schema/seed: Flyway `module-server`; seed RBAC/org/person demo → `module-auth-bom/src/main/resources/data/`.

---

## 18. Cross-links

| Tài liệu / module | Liên hệ |
|-------------------|---------|
| [module-auth-bom.md](./module-auth-bom.md) | Login, OTP quên MK, gọi `IpBlockService` |
| [module-common.md](./module-common.md) | `IpBlockService`, `BlockReason`, Notification, Comment, Audit, Workflow |
| [module-server.md](./module-server.md) | NotificationImpl, DashboardImpl, Flyway |
| [module-email-bom.md](./module-email-bom.md) | SMTP OTP / weekly report |
| [module-qtbv-bom.md](./module-qtbv-bom.md) | Website/CMS public (không nằm QTHT) |
| [module-dmdc-bom.md](./module-dmdc-bom.md) | Danh mục dùng chung (`/qtht/category`) — module riêng |
| [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) | Naming, UUID, soft delete, index |

```
[Auth OTP fail ×5]
  → IpBlockServiceImpl → block_ip + users.status=0 + ip_blacklist
[Admin]
  → PUT /qtht/user/{id}/active
  → DELETE /qtht/ip-blacklist/unban/{id}
[FE ERP]
  → GET /qtht/menu/user/{username}
  → POST /qtht/usage/pageview
```

---

## 19. Checklist

### 19.1 Đọc code lần đầu

- [ ] Controllers theo bảng mục 2 — biết base path từng domain
- [ ] `User` ở auth vs `Person` / `UserAdminController` ở qtht
- [ ] `MenuService` sidebar + `Person.isAdmin` bypass
- [ ] `IpBlockServiceImpl` + 4 bảng IP (mục 7)
- [ ] `ApiLog` / `SystemJob` / `DynamicJobScheduler`
- [ ] `NotificationController` / `CommentController` / Workflow = facade
- [ ] `GuideController` ≠ public website (`module-qtbv-bom`)

### 19.2 Rule

| ✅ | ❌ |
|----|----|
| Soft-delete theo `BaseEntity` | Hard delete org/dept/menu khi còn tham chiếu |
| Khóa OTP qua `IpBlockService` | Set `users.status` thẳng từ chỗ khác không audit |
| Combobox JWT-only khi cần bootstrap UI | Gắn CheckPermission làm treo sidebar |
| Job đổi cron qua `/qtht/jobs` + scheduler sync | Hard-code cron chỉ trong class mà admin không thấy |

---

*Cập nhật khi thêm controller domain mới, đổi threshold IP/OTP, đổi catalog job, hoặc tách facade workflow/comment sang module khác.*
