# Frezo Backend — module-qlns-bom (HRM / QLNS)

> Module quản lý nhân sự: hợp đồng, chấm công, nghỉ phép, kỳ lương/payslip, recognition token, onboarding/offboarding, tuyển dụng, OKR/performance.
> Đọc **cùng** [module-approval-bom.md](./module-approval-bom.md) · [module-qtht-bom.md](./module-qtht-bom.md) · [module-common.md](./module-common.md) · [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

Package gốc: `com.frezo.qlns`. Module Maven: **`module-qlns-bom`**.

**Person master data không nằm trong module này** — entity/API ở `module-qtht-bom` (`Person`, `PersonController` path `/qlns/person`). QLNS chỉ giữ FK `person_id` / `contract_id`.

---

## 1. Vai trò trong FrezoBE

| Hạng mục | Chi tiết |
|----------|----------|
| Phạm vi | HR operational: employment lifecycle, time & leave, payroll, recognition, recruitment hubs |
| Không thuộc module | Org chart / Role / User CRUD (qtht/auth); approval inbox engine (`module-approval-bom`); GL journal core (`module-accounting-bom`) |
| Prefix API | `/qlns/*` (24 controllers trong qlns; Person API cũng `/qlns/person` nhưng code ở qtht) |
| Approval gắn domain | Leave + khoá kỳ lương qua `ApprovalCreator` + `QlnsApprovalSubjectListener` |

```
Person (qtht) ──person_id──► Attendance / Leave / Payroll / Onboarding / Resignation / Token / OKR
     │
     └── Contract (qlns) ──contract_id──► LeaveRequest / Attendance / Payroll
              │
              └── ContractSignSession (OTP ký số)
```

---

## 2. Cấu trúc package

| Package | Vai trò |
|---------|---------|
| `controller` | REST `/qlns/...` |
| `service` / `service.Impl` | Business (folder `Impl`, package thường `service.impl`) |
| `service.Impl.payroll` | Orchestrator, lifecycle, enricher, config loader, detail writer, GL posting |
| `entity` / `repository` / `mapper` | JPA + MapStruct |
| `dto.request` / `dto.response` | API DTO |
| `common` | `QlnsErrorCode`, `StatusContarct`, `AttendanceStatus` |
| `engine` | `PayrollEngine` — tính lương thuần |
| `job` | `AttendanceReminderJob` |
| `listener` | `QlnsApprovalSubjectListener` |
| `config` | `ContractDataInitializer`, `ContractStatusSchemaFixer`, `PayrollDataInitializer` |
| `recognition` | `RecognitionConfig` |
| `recruitment` | `RecruitmentConstants`, `RecruitmentErrorCode` |

---

## 3. Dependencies Maven

| Dependency | Dùng cho |
|------------|----------|
| `module-qtht-bom` | `Person`, `Department`, `Organization`, `Setting`, `PushNotificationService`, `UserAdminService` (hire) |
| `module-common` | `BaseEntity`, `NotificationService`, `SubjectType`, `SchedulableJob`, exception/permission |
| `module-accounting-bom` | `JournalService`, `AccountingSetting`, `PayslipConfirmationService` |
| `module-approval-bom` | `ApprovalCreator`, `ApprovalDecidedEvent` |
| `module-auth-bom` | Transitive (User / hire register) |
| `module-email-bom` | Transitive qua qtht — OTP ký HĐ / payslip email |

**Rule:**
- ✅ Domain approval qua `ApprovalCreator` + listener sync status
- ❌ Không hardcode flowId khi create leave/payroll — runtime lấy flow **active** theo `subjectType` (xem [module-approval-bom.md](./module-approval-bom.md))

---

## 4. Person (master) — nằm ở QTHT

> Chi tiết CRUD Person thuộc QTHT; mục này ghi cách QLNS phụ thuộc.

| Hạng mục | Chi tiết |
|----------|----------|
| Entity | `com.frezo.qtht.entity.Person` → bảng `person` |
| Controller | `PersonController` — `@RequestMapping("/qlns/person")` trong **`module-qtht-bom`** |
| Cột chính | `code`, `name`, `activated`, `email`, `phone`, `job_title`, `is_admin`, `department_id`, `org_id` |
| Offboarding | `ResignationRequestServiceImpl.complete()` → `PersonService.deactivate(personId)` |

API Person (qtht): `GET /all`, `POST /`, `PUT /{id}`, `GET /combobox`, `GET /{id}`, `PUT /{id}/activate|deactivate`, `DELETE /{id}`, `POST /upload-avatar-temp`.

Tài liệu phụ: `PersonDocumentController` `/qtht/person-document` (không phải HR core trong qlns-bom).

---

## 5. Bản đồ Controller (API index)

| Controller | Base path | Domain |
|------------|-----------|--------|
| *(qtht)* `PersonController` | `/qlns/person` | Master NS |
| `ContractController` | `/qlns/contract` | Hợp đồng |
| `ContractSignController` | `/qlns/contracts` | OTP ký số |
| `ContractTemplateController` | `/qlns/contract-template` | Template HĐ |
| `AttendanceController` | `/qlns/attendance` | Chấm công |
| `RegularizationController` | `/qlns/attendance-regularization` | Bù công |
| `TimesheetReportController` | `/qlns/report` | Báo cáo công |
| `LeaveController` | `/qlns/leave` | Leave legacy |
| `LeaveRequestController` | `/qlns/leave-request` | Leave + approval |
| `PayrollController` | `/qlns/payroll` | Tính / confirm / pay |
| `PayrollPeriodController` | `/qlns/payroll-period` | Kỳ lương + lock |
| `PayrollConfigController` | `/qlns/payroll-config` | Config BH/tax/OT |
| `PayslipController` | `/qlns/payslip` | Phiếu lương + GL |
| `RecognitionController` | `/qlns/recognition` | Token |
| `OnboardingController` | `/qlns/onboarding` | Onboarding |
| `ResignationRequestController` | `/qlns/resignation` | Offboarding |
| `EmployeeDependentController` | `/qlns/employee-dependent` | Người phụ thuộc |
| `OkrController` | `/qlns/okrs` | OKR |
| `PerformanceReviewController` | `/qlns/performance-reviews` | Đánh giá |
| `RequisitionController` | `/qlns/recruitment/requisitions` | YC tuyển |
| `CandidateController` | `/qlns/recruitment/candidates` | Ứng viên |
| `JobApplicationController` | `/qlns/recruitment/applications` | Hồ sơ ứng tuyển |
| `InterviewController` | `/qlns/recruitment/interviews` | Phỏng vấn |
| `OfferController` | `/qlns/recruitment/offers` | Offer |
| `RecruitmentMetaController` | `/qlns/recruitment` | `GET /stages` |

---

## 6. Hợp đồng (Contract)

### 6.1 Entities

| Bảng | Entity | Ghi chú |
|------|--------|---------|
| `contract` | `Contract` | Neo employment: `person_id`, `code`, `status` (`StatusContarct` STRING), `activated`, lương/`value`, HTML, AI fields |
| `contract_history` | `ContractHistory` | Lịch sử status |
| `contract_templates` | `ContractTemplate` | File template |
| `contract_assgin_work` | `ContractAssginWork` | Gán công việc (tên bảng giữ typo `assgin`) |
| `contract_version_history` | `ContractVersionHistory` | Version / diff |
| `contract_sign_session` | `ContractSignSession` | OTP ký số |

### 6.2 Status — `StatusContarct`

`DRAFT`, `PENDING_APPROVAL`, `NEGOTIATING`, `NO_YEP_EFFECTIVE`, `ACTIVE`, `SUSPENDED`, các bước OP/RV (`WAITING_FOR_OP`, `OP_PROCESSING`, `WAITING_FOR_RV`, `RV_REVIEWING`, `OP_DONE`, `RV_DONE`, `RV_REJECTED`, `OP_REWORK`), `COMPLETED`, `CANCELLED`.

Leave yêu cầu hợp đồng **active**: `activated == true` **và** `status == ACTIVE` (`LeaveApprovalBridge.assertActiveContract`).

### 6.3 API chính — `/qlns/contract`

| Method | Path | Ý nghĩa |
|--------|------|---------|
| POST / PUT / DELETE | `/`, `/{id}` | CRUD |
| GET | `/`, `/combobox`, `/{id}` | List / detail |
| POST / GET | `/{contractId}/assign` | Gán công việc |
| PUT | `/{id}/update-status`, `/{id}/reject` | Workflow OP/RV |
| POST | `/upload`, `/upload-and-extract`, `/{id}/save-content`, `/{id}/ai-edit`, `/ai-edit` | Upload / AI |
| GET | `/{id}/ai-status`, `/{contractId}/versions`, `.../versions/diff` | AI + version |

Services: `ContractServiceImpl`, `ContractTemplateServiceImpl`, `ContractVersionServiceImpl`, `AiExtractionService` (cần verify đầy đủ surface AI).

Config startup: `ContractStatusSchemaFixer` (ORDINAL → VARCHAR), `ContractDataInitializer`.

---

## 7. Ký số hợp đồng (OTP)

### 7.1 API — `/qlns/contracts`

| Method | Path | Service |
|--------|------|---------|
| GET | `/{id}/sign/status` | `ContractSignService.status` |
| POST | `/{id}/sign/request-otp` | `requestOtp` |
| POST | `/{id}/sign/confirm` | `confirm` |

### 7.2 Flow

```
requestOtp(contractId)
  → sinh OTP 6 số, hash SHA-256 → contract_sign_session (PENDING, TTL 10 phút)
  → NotificationService.notifyUserWithEmailFallback(username, title, msg chứa OTP, urgent=true)
  → catch: log WARN + log OTP (dev)

confirm(contractId, otp)
  → load session PENDING mới nhất
  → hết hạn → EXPIRED + lỗi
  → sai hash → lỗi
  → SIGNED + signed_at / signed_by / ip / device
```

Session status: `PENDING` → `SIGNED` | `EXPIRED`. Poll không có session → `UNSIGNED` (cần verify payload exact trong `status()`).

**Rule:**
- ✅ OTP hash trong DB, plaintext chỉ trong mail/message notify
- ❌ `confirm` **không** tự đổi `contract.status` / `activated` — chỉ cập nhật session (FE/luồng khác kích hoạt HĐ nếu cần)
- ⚠ Dev path: khi email fail, OTP có thể xuất hiện trong application log — cần verify policy prod

---

## 8. Chấm công (Attendance)

### 8.1 Entities

| Bảng | Cột chính |
|------|-----------|
| `attendance` | Unique `(person_id, attendance_date)`; check-in/out + GPS/WiFi; `work_minutes`, `late_minutes`, `overtime_minutes`; `status` (`AttendanceStatus`) |
| `attendance_regularization` | Yêu cầu bù giờ; `status`; `manager_username` |

`AttendanceStatus`: `PRESENT`, `ABSENT`, `LATE`, `HALF_DAY`, `LEAVE`, `HOLIDAY`.

### 8.2 API

**`/qlns/attendance`:** `POST /check-in`, `POST /check-out`, `GET`, `GET /daily`, `GET /stats`, `GET /{id}`

**`/qlns/attendance-regularization`:** `POST`, `GET /my/{personId}`, `GET /pending`, `PUT /{id}/approve`, `PUT /{id}/reject`

**`/qlns/report`:** `GET /timesheet`, `GET /timesheet/export`

### 8.3 Flow check-in / check-out

1. Check-in idempotent nếu đã check-in trong ngày
2. Validate GPS (Haversine) + WiFi allowlist từ `qtht.Setting.details` (cần verify key setting exact)
3. Tính `lateMinutes` so với ca sáng/chiều → `LATE` hoặc `PRESENT`
4. Check-out: bắt buộc đã check-in → `workMinutes`, `overtimeMinutes`

### 8.4 Regularization

Duyệt **1 cấp** trên API qlns (manager/HR) — **không** gắn `module-approval`. Approve ghi đè/tạo `Attendance` ngày đó + `NotificationService`.

### 8.5 Job nhắc — `AttendanceReminderJob`

| Field | Value |
|-------|-------|
| Code | `ATTENDANCE_REMINDER` |
| Module | `QLNS` |
| Cron mặc định | `0 */5 * * * MON-FRI` (seed `system_job` — migration `V202608051610__system_job.sql`) |
| Check-in remind | ~15 phút trước `morningStart` (default 08:00) nếu chưa vào |
| Check-out remind | ~30–90 phút sau `afternoonEnd` (default 17:30) nếu đã vào chưa ra |
| Kênh | `PushNotificationService` (qtht) |

---

## 9. Nghỉ phép (Leave)

### 9.1 Hai model song song

| | `LeaveRecord` (legacy) | `LeaveRequest` (hiện tại) |
|--|------------------------|---------------------------|
| Bảng | `leave_record` | `leave_request` |
| API | `/qlns/leave` | `/qlns/leave-request` |
| Service | `LeaveServiceImpl` | `LeaveRequestServiceImpl` |
| Duyệt | Inline `approve` trên qlns | **`POST /approvals/{id}/approve\|reject`** |
| Status | `PENDING` / `APPROVED` / `REJECTED` | `PENDING_MANAGER` / `PENDING_HR` / `APPROVED` / `REJECTED` / `CANCELLED` (+ legacy `PENDING`) |

Bảng phụ: `leave_request_history`, `leave_balance` (`person_id` + `year` — **cần verify** service có cập nhật balance tự động hay chưa).

### 9.2 LeaveRequest + Approval

```
LeaveRequestServiceImpl.create
  → LeaveApprovalBridge.assertActiveContract(contractId)
  → status = PENDING_MANAGER, resolve managerUsername (LeaveApprovalResolver)
  → LeaveApprovalBridge.start(leave)
       → ApprovalCreator.create(SubjectType.LEAVE, leaveId, summary, null, null, null)
       → leave.approvalRequestId = req.id
  → notify manager (QLNS-owned, không phải ApprovalDecidedEvent)

Duyệt cũ PUT /qlns/leave-request/{id}/approve|reject → HTTP 410
```

Listener `QlnsApprovalSubjectListener.syncLeave`:

| Event status | Leave status |
|--------------|--------------|
| `ASSIGNED` | `PENDING_HR` (+ manager approved fields) |
| `APPROVED` | `APPROVED` (+ HR fields) |
| `REJECTED` | `REJECTED` |

Chi tiết engine: [module-approval-bom.md](./module-approval-bom.md). Flow seed tham chiếu: `LEAVE_STANDARD` (`subjectType=LEAVE`).

### 9.3 API LeaveRequest

| Method | Path | Ghi chú |
|--------|------|---------|
| POST | `/qlns/leave-request` | Tạo + start approval |
| GET | `/my/{contractId}`, `/pending`, `/{id}/history` | List / history |
| PUT | `/{id}/approve`, `/{id}/reject` | **DEPRECATED 410** |
| PUT | `/{id}/cancel` | Huỷ khi PENDING_* |

IDOR: `assertCanViewContractLeaves` / `assertCanViewLeave` — admin / HR / permission `QLNS_LEAVE_*` / `APPROVALS_APPROVE` (cần verify danh sách permission seed).

---

## 10. Lương / Kỳ lương / Payslip

### 10.1 Entities

| Bảng | Ý nghĩa |
|------|---------|
| `payroll` | Bản ghi lương theo person + month/year; `status` int 0/1/2 |
| `payroll_detail` | Dòng EARNING / DEDUCTION |
| `payroll_period` | Kỳ org+month+year; `status` 0 OPEN / 1 LOCKED / 2 CLOSED; `approval_request_id`; legacy `workflow_instance_id` |
| `payroll_config` / `insurance_config` / `tax_config` | Tham số tính |
| `employee_dependent` | Số người phụ thuộc → giảm trừ thuế |

### 10.2 Status

| Entity | 0 | 1 | 2 |
|--------|---|---|---|
| `payroll` | Draft | Confirmed | Paid |
| `payroll_period` | OPEN | LOCKED | CLOSED |

### 10.3 Kiến trúc service

```
PayrollServiceImpl (facade)
├── PayrollCalculationOrchestrator → PayrollEngine + PayrollDataCollector + RecognitionService
├── PayrollLifecycleService        → confirm / pay / delete + lock guard
├── PayrollEnricher / PayrollDetailWriter / PayrollConfigLoader
├── PayrollPeriodServiceImpl       → CRUD + lock → ApprovalCreator(PAYROLL)
├── PayslipServiceImpl
└── PayrollGLPostingServiceImpl    → JournalService (accounting)
```

`PayrollEngine.calculate`: gross (ngày công + OT + phụ cấp + bonus − phạt muộn) → BH → công đoàn → thuế lũy tiến → net. Token redeem đã `APPROVED` được `RecognitionService.consumeApprovedForPayroll` cộng tiền rồi đánh dấu `PAID`.

### 10.4 API

**`/qlns/payroll`:** `POST /calculate/{personId}`, `POST /calculate-all`, `PUT /{id}/bonus|confirm|pay`, `DELETE /{id}`, details/list/export payslip/bank

**`/qlns/payroll-period`:** CRUD, `PUT|POST /{id}/lock`, `unlock`, `close`

**`/qlns/payroll-config`:** `/payroll`, `/insurance`, `/tax` GET/POST

**`/qlns/payslip`:** `GET /{payrollId}`, `GET /ytd`, `GET /formulas`, `POST /{payrollId}/confirm`, `POST /period/{year}/{month}/post-to-gl`, `.../reverse-gl`

### 10.5 Lock kỳ lương → Approval

```
PayrollPeriodServiceImpl.lock(id)
  → status = LOCKED
  → ApprovalCreator.create(SubjectType.PAYROLL, periodId, …)
  → approval_request_id

QlnsApprovalSubjectListener.syncPayroll:
  APPROVED  → CLOSED
  REJECTED  → OPEN + clear lock + clear approval_request_id
  ASSIGNED  → LOCKED chỉ khi status null (thường lock() đã set sẵn)
```

Flow seed: `PAYROLL_PERIOD` (`subjectType=PAYROLL`). Có **song song** legacy `WorkflowService` / `workflow_instance_id` — cần verify API cũ còn expose hay không trước khi xoá.

### 10.6 GL posting

`PayrollGLPostingServiceImpl.postPeriod(month, year)`:

- Idempotency key dạng `payroll:{year}-{month}`
- Gộp toàn bộ payroll kỳ → một journal `JournalService.createAndPost`
- `PostingSource.PAYROLL`; tài khoản từ `AccountingSetting` (vd 6421, 334, 3383–3385, 3335, 3382 — **cần verify** mapping hiện tại trong code/setting)
- Reverse: `journalService.reverse()`

Payslip confirm nhận phiếu: `PayslipConfirmationService` (accounting-bom).

---

## 11. Recognition (Token)

### 11.1 Entities / config

| Bảng | Vai trò |
|------|---------|
| `token_wallet` | `person_id` unique, `balance` |
| `token_transfer` | Gift / nguồn `MANUAL` / `TASK` |
| `token_redeem_request` | Đổi token → tiền lương; status `PENDING`/`APPROVED`/`REJECTED`/`PAID` |
| `token_reward_catalog` | Catalog phần thưởng |

`RecognitionConfig`: `TOKEN_TO_VND=1000`, `MAX_GIFT_AMOUNT=100`, `MAX_REDEEM_AMOUNT=10000`, `STARTER_BALANCE=50`.

### 11.2 API — `/qlns/recognition`

`GET /config`, `/wallet/me`, `/wallet/{personId}`, `/wallets`, `POST /gift`, `GET /transfers`, `POST /redeem`, `GET /redeem`, `POST /redeem/{id}/approve|reject`, `GET /catalog`.

Duyệt redeem **inline** (HR trên qlns) — không qua approval engine.

---

## 12. Onboarding

| Bảng | Vai trò |
|------|---------|
| `onboarding_template` / `onboarding_template_item` | Mẫu checklist (`due_day_offset`, `assignee_role`) |
| `onboarding_assignment` / `onboarding_assignment_item` | Gán cho `person_id`; item `PENDING`/`DONE`/`SKIPPED` |

Assignment: `IN_PROGRESS` → `COMPLETED` khi mọi item DONE/SKIPPED.

API `/qlns/onboarding`: templates CRUD, assignments list/create, `POST /assignments/{assignmentId}/items/{itemId}/complete`.

---

## 13. Offboarding — Resignation

Bảng `resignation_request` (Flyway `V202607291100__resignation_request.sql`).

```
REQUESTED → approve → APPROVED → handover → HANDOVER_DONE
  → settle-payroll → PAYROLL_SETTLED → complete → COMPLETED (+ Person.deactivate)
Any non-terminal → cancel → CANCELLED
```

API `/qlns/resignation`: `POST`, `GET`, `GET /{id}`, `POST /{id}/approve|handover|settle-payroll|complete|cancel`.

**Không** gắn `module-approval`. `settlePayroll` là milestone thủ công (không auto gọi payroll calc trong code hiện tại).

---

## 14. Tuyển dụng (Recruitment) — tóm tắt

| Bảng | Status / notes |
|------|----------------|
| `recr_requisition` | `OPEN` / `ON_HOLD` / `FILLED` / `CLOSED` |
| `recr_candidate` | Master ứng viên |
| `recr_application` | Stage pipeline |
| `recr_interview` | PHONE/ONLINE/ONSITE/… ; SCHEDULED/DONE/… |
| `recr_offer` | DRAFT → SENT → ACCEPTED/REJECTED/EXPIRED |

Stage transitions (`RecruitmentConstants`): `APPLIED → SCREENING → INTERVIEW → OFFER → HIRED | REJECTED`.

Hire (`JobApplicationServiceImpl.markHired`): có thể tạo User (`UserAdminService.register`) + link Person theo email; đóng requisition `FILLED` khi đủ quantity. Config: `qlns.recruitment.hire.require-user-account` (default true — cần verify trong yml).

API dưới `/qlns/recruitment/*` + `GET /qlns/recruitment/stages`.

---

## 15. OKR / Performance / Dependent — tóm tắt

| Domain | Bảng | API | Ghi chú |
|--------|------|-----|---------|
| OKR | `okr`, `okr_key_result` | `/qlns/okrs` + `POST /{id}/check-in` | Scope `mine`/`team`/`all` (`OkrScopeResolver`); status DRAFT/ACTIVE/COMPLETED/CANCELLED |
| Performance | `performance_cycle`, `performance_review` | `/qlns/performance-reviews` | DRAFT → SUBMITTED → SCORED |
| Dependent | `employee_dependent` | `/qlns/employee-dependent` | Feed số NPT cho `PayrollEngine` |

---

## 16. Jobs / Listeners / Engines

| Loại | Class | Việc |
|------|-------|------|
| Job | `AttendanceReminderJob` | Nhắc check-in/out |
| Listener | `QlnsApprovalSubjectListener` | Sync Leave + PayrollPeriod từ `ApprovalDecidedEvent` |
| Engine | `PayrollEngine` | Tính lương thuần |
| Bridge | `LeaveApprovalBridge`, `LeaveApprovalResolver` | Gắn approval + resolve manager/HR/IDOR |
| Config | `Contract*`, `PayrollDataInitializer` | Seed / schema fix |

---

## 17. Bảng DB đáng chú ý & migration

### 17.1 Flyway đã thấy (module-server)

| Migration | Nội dung |
|-----------|----------|
| `V202607211000__contract_status_varchar.sql` | `contract.status` (+ history) int → VARCHAR |
| `V202607291100__resignation_request.sql` | Tạo `resignation_request` |
| `V202608051610__system_job.sql` | `system_job` + seed `ATTENDANCE_REMINDER` |

### 17.2 Phần lớn bảng QLNS

`leave_*`, `payroll*`, `attendance*`, `onboarding_*`, `token_*`, `recr_*`, … — **không** thấy đủ Flyway trong repo; khả năng tạo bởi `ddl-auto=update` / schema legacy. Production `ddl-auto=validate` → **cần verify** schema đã khớp entity trước deploy.

Sample: `module-qlns-bom/src/main/resources/data/payroll_sample_data.sql`.

---

## 18. Gắn Approval — tóm tắt QLNS

| SubjectType | Entry | Flow seed (tham chiếu) | Sync listener |
|-------------|-------|------------------------|---------------|
| `LEAVE` | `LeaveApprovalBridge.start` | `LEAVE_STANDARD` | Leave → PENDING_HR / APPROVED / REJECTED |
| `PAYROLL` | `PayrollPeriodServiceImpl.lock` | `PAYROLL_PERIOD` | Period → CLOSED / OPEN |

Constant trong code (`FLOW_CODE`, `APPROVAL_FLOW_CODE`) chủ yếu **tham chiếu**; runtime truyền `flowId=null, flowCode=null` → lấy flow **active** theo subjectType.

Không dùng approval: regularization, resignation, recognition redeem, contract OP/RV nội bộ.

Chi tiết đầy đủ: [module-approval-bom.md](./module-approval-bom.md).

---

## 19. Cross-links module khác

| Module / guild | Liên hệ |
|----------------|---------|
| [module-approval-bom.md](./module-approval-bom.md) | Inbox duyệt Leave / khoá kỳ lương |
| [module-qtht-bom.md](./module-qtht-bom.md) | Person, Setting GPS/WiFi, Push, Role (approver) |
| [module-common.md](./module-common.md) | `NotificationService`, `SubjectType`, `BaseEntity` |
| [module-email-bom.md](./module-email-bom.md) | OTP ký HĐ / notify urgent (qua Notification) |
| [module-server.md](./module-server.md) | Wiring app, Flyway, `NotificationServiceImpl` |
| `module-accounting-bom` | GL post payslip, `PayslipConfirmation` |
| `module-warehouse-bom` | Cùng pattern `ApprovalCreator` (PR) — tham chiếu khi đọc approval |

---

## 20. Checklist

### 20.1 Đọc code lần đầu

- [ ] `Person` + `/qlns/person` nằm ở **qtht**, không có PersonController trong qlns
- [ ] `Contract` + `StatusContarct.ACTIVE` / `activated` — điều kiện leave
- [ ] `ContractSignServiceImpl` — OTP hash, TTL 10p, không đổi status HĐ
- [ ] Attendance check-in/out + `AttendanceReminderJob` + regularization (duyệt local)
- [ ] Phân biệt `/qlns/leave` vs `/qlns/leave-request` (410 trên approve cũ)
- [ ] `LeaveApprovalBridge` → `ApprovalCreator` → `QlnsApprovalSubjectListener`
- [ ] Payroll: Engine → Lifecycle → Period.lock → Approval → GL post
- [ ] Recognition redeem → `consumeApprovedForPayroll`
- [ ] Onboarding assignment items; Resignation → `PersonService.deactivate`
- [ ] Recruitment hire tạo User (nếu config bật)

### 20.2 Rule bắt buộc

| ✅ | ❌ |
|----|----|
| Duyệt leave / khoá kỳ lương qua `/approvals/*` | Gọi lại PUT approve leave trên qlns (410) |
| Fail-fast khi thiếu approver role (approval) | Tạo phiếu PENDING treo ngoài inbox |
| Soft-link Person qua `person_id` string | Copy master Person vào qlns |
| Ghi nhận: sign session ≠ activate contract | Giả định confirm OTP = HĐ ACTIVE |

### 20.3 Cần verify trong code / môi trường

- [ ] `leave_balance` có được trừ khi APPROVED không
- [ ] Mapping tài khoản GL payslip trên từng env
- [ ] Legacy `WorkflowService` trên payroll còn được FE gọi không
- [ ] Schema prod đủ bảng khi `ddl-auto=validate`
- [ ] Permission codes seed cho leave / payroll / recognition

---

*Cập nhật khi đổi approval subject types, status máy trạng thái leave/payroll, hoặc tách Person khỏi path `/qlns/person`.*
