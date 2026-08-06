# Frezo Backend — module-approval-bom (Approval / Workflow engine)

> Engine duyệt đa bước dùng chung: template flow (`approval_flow` + `approval_flow_step`), runtime request (`approval_request` + `approval_step`), inbox API, Spring event đồng bộ domain.
> Đọc **cùng** [module-qlns-bom.md](./module-qlns-bom.md) · [module-common.md](./module-common.md) · [module-qtht-bom.md](./module-qtht-bom.md) · [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

Package gốc: `com.frezo.approval`. Module Maven: **`module-approval-bom`**.

Consumer chính: **QLNS** (Leave, khoá kỳ lương), **Warehouse** (Purchase Request). Không thay thế mọi workflow nội bộ (contract OP/RV, resignation, regularization vẫn duyệt local).

---

## 1. Vai trò

| Hạng mục | Chi tiết |
|----------|----------|
| Template | Admin cấu hình bước duyệt theo `subjectType` (1 flow **active** / loại) |
| Runtime | Tạo request + steps; approve/reject từng bước; inbox “của tôi” |
| Tích hợp | Module nghiệp vụ gọi `ApprovalCreator`; lắng nghe `ApprovalDecidedEvent` để sync status |
| Notify | `ApprovalNotificationListener` → `NotificationService` |

```
Business module                    module-approval-bom
─────────────────                  ───────────────────
LeaveApprovalBridge ──┐
PayrollPeriod.lock ───┼──► ApprovalCreator ──► approval_request + steps
PurchaseRequest.submit┘              │
                                     │ (approve/reject qua API)
                                     ▼
                          ApprovalServiceImpl
                                     │
                                     ▼
                          ApprovalDecidedEvent
                          ├── ApprovalNotificationListener
                          ├── QlnsApprovalSubjectListener
                          └── PurchaseRequestApprovalListener
```

---

## 2. Cấu trúc package

| Package | Class chính | Vai trò |
|---------|-------------|---------|
| `controller` | `ApprovalController`, `ApprovalFlowController` | REST |
| `service` | `ApprovalCreator`, `ApproverResolver`, `ApprovalService`, `ApprovalFlowService` | Core |
| `service.impl` | `ApprovalServiceImpl`, `ApprovalFlowServiceImpl` | Impl |
| `entity` | 4 entities | Persistence |
| `repository` | 4 repos | JPA |
| `dto` | Create / Flow / Step / Action / Request DTOs | API |
| `event` | `ApprovalDecidedEvent` | Spring event |
| `listener` | `ApprovalNotificationListener` | Push/inbox notify |
| `config` | `ApprovalFlowSeedRunner` | Seed flow chuẩn |
| `common` | `ApprovalErrorCode` | Error codes |

---

## 3. Dependencies Maven

| Dependency | Dùng cho |
|------------|----------|
| `module-common` | `BaseEntity`, `ApiResponse`, `FePage`, `CheckPermission`, `AppException`, `SystemUtils`, `NotificationService`, `SubjectType` |
| `module-auth-bom` | `User`, `UserRole`, repos — resolve approver |
| `module-qtht-bom` | `Role`, `RoleRepository` — map `approver_role` → users |
| `spring-boot-starter-data-jpa` / `web` | Persistence + REST |

Consumers phụ thuộc module này: `module-qlns-bom`, `module-warehouse-bom`, `module-server` (scan).

---

## 4. Entities & bảng

Tất cả kế thừa `BaseEntity` (id UUID string, audit, soft delete).

### 4.1 `approval_flow` — `ApprovalFlow`

| Cột | Ý nghĩa |
|-----|---------|
| `code` | Seed: `LEAVE_STANDARD`, `PAYROLL_PERIOD`, `PURCHASE_REQUEST`; API create: `FLOW-{timestamp}` |
| `name` | Tên hiển thị |
| `subject_type` | `LEAVE`, `PAYROLL`, `PURCHASE_REQUEST`, … |
| `active` | Chỉ **một** flow active / `subject_type` khi vận hành |
| `description` | Mô tả |

Index: `idx_appr_flow_subject` (`subject_type`).

### 4.2 `approval_flow_step` — `ApprovalFlowStep`

| Cột | Ý nghĩa |
|-----|---------|
| `flow_id` | FK flow |
| `step_order` | Thứ tự 1..N |
| `approver_role` | Mã role QTHT: `MANAGER`, `HR`, `CHIEF_ACC`, `ADMIN`, … |
| `name` | Nhãn bước |

### 4.3 `approval_request` — `ApprovalRequest`

| Cột | Ý nghĩa |
|-----|---------|
| `flow_id` | Template đã chọn |
| `subject_type` / `subject_id` | Bản ghi nghiệp vụ |
| `subject_summary` | Text inbox |
| `requested_by` / `requested_at` | Người gửi |
| `current_step_order` / `total_steps` | Tiến độ |
| `status` | `PENDING` / `APPROVED` / `REJECTED` (Javadoc còn `CANCELLED` — **chưa implement** trong service) |
| `current_approver_hint` | Username hoặc role gợi ý UI |

Indexes: `idx_appr_req_status`, `idx_appr_req_subject` (`subject_type`, `subject_id`).

### 4.4 `approval_step` — `ApprovalStep`

| Cột | Ý nghĩa |
|-----|---------|
| `request_id` | Parent |
| `step_order` | `0` = SUBMITTED; `1..N` = duyệt |
| `approver_role` / `approver_person_id` / `approver_username` / `approver_name` | Assign lúc create |
| `action` | `SUBMITTED` / `PENDING` / `APPROVED` / `REJECTED` (Javadoc `SKIPPED` — unused) |
| `comment` / `actioned_at` | Khi action |

---

## 5. Status & SubjectType

### 5.1 Chuỗi status (không có Java enum riêng)

| Layer | Giá trị dùng trong code |
|-------|-------------------------|
| Request `status` | `PENDING`, `APPROVED`, `REJECTED` |
| Step `action` | `SUBMITTED`, `PENDING`, `APPROVED`, `REJECTED` |
| Event `ApprovalDecidedEvent.status` | `ASSIGNED`, `APPROVED`, `REJECTED` |

**Phân biệt:** Event `ASSIGNED` = chuyển bước (chưa phải request APPROVED). Listener domain map `ASSIGNED` → trạng thái trung gian (vd leave `PENDING_HR`).

### 5.2 `SubjectType` (`module-common`)

Enum có nhiều giá trị (`LEAVE`, `PAYROLL`, `DEAL`, `INVOICE`, `TICKET`, `CONTRACT`, `QUOTE`, `PURCHASE_REQUEST`, `RECRUITMENT`, `GENERIC`).

**Engine đang gắn thật:** `LEAVE`, `PAYROLL`, `PURCHASE_REQUEST`. Các giá trị còn lại — cần verify nếu FE/admin tạo flow.

### 5.3 `ApprovalErrorCode`

| Code | Khi nào |
|------|---------|
| `FLOW_NOT_FOUND` | Không resolve được flow theo id/code/subjectType |
| `FLOW_EMPTY` | Flow không có bước |
| `NO_APPROVER` | Role bước không có user active — fail-fast chống treo inbox |

---

## 6. API Controllers

### 6.1 Inbox & quyết định — `/approvals` (`ApprovalController`)

| Method | Path | Permission (action) | Mục đích |
|--------|------|---------------------|----------|
| GET | `/approvals/my?status=pending` | VIEW | Inbox: mặc định PENDING mà user là current approver; status khác → related (requester hoặc từng là approver) |
| POST | `/approvals/{id}/approve` | APPROVE | Duyệt bước hiện tại; body optional `ApprovalActionPayload.comment` |
| POST | `/approvals/{id}/reject` | APPROVE | Từ chối — terminal |
| GET | `/approvals/{id}/timeline` | VIEW | Timeline 1 request |
| GET | `/approvals/timeline?subjectType&subjectId` | VIEW | Timeline theo subject (embed UI) |
| GET | `/approvals/by-subject?subjectType&subjectId` | VIEW | Latest request DTO |
| POST | `/approvals` | CREATE | HTTP create → `ApprovalCreator` + publish `ASSIGNED` |

`listMy` trả `FePage.all(...)` — **không** phân trang server thật (cần verify khi data lớn).

### 6.2 Template — `/approval-flows` (`ApprovalFlowController`)

| Method | Path | Action | Mục đích |
|--------|------|--------|----------|
| GET | `/approval-flows` | VIEW | List flow + steps |
| POST | `/approval-flows` | CREATE | Tạo flow; `code` auto `FLOW-{ts}`; `active` default true → deactivate siblings cùng subjectType |
| PUT | `/approval-flows/{id}` | UPDATE | Patch metadata / thay steps (soft-delete step cũ); `active=true` → deactivate others |

**Không** có endpoint `/activate` riêng — kích hoạt bằng `PUT` với `"active": true`.

**DTO gap:** `ApprovalFlowDto` có thể **không** expose field `code` ra FE — cần verify khi admin cần nhận diện seed vs custom.

---

## 7. `ApprovalCreator` — tạo request

File: `com.frezo.approval.service.ApprovalCreator`

### 7.1 Signature

```java
ApprovalRequest create(String subjectType, String subjectId, String subjectSummary,
                       String flowId, String requestedBy);

ApprovalRequest create(String subjectType, String subjectId, String subjectSummary,
                       String flowId, String flowCode, String requestedBy);
```

### 7.2 Resolve flow (ưu tiên)

1. `flowId` non-blank → `findById` (chưa deleted)
2. `flowCode` non-blank → active by code, fallback by code
3. Else → `findFirstBySubjectTypeAndActiveTrueAndIsDeletedFalse(subjectType)`

Integrations Leave / Payroll / PR đều truyền `flowId=null`, `flowCode=null` → **active theo subjectType**.

### 7.3 Thuật toán create

1. Load steps template theo `step_order` ASC; empty → `FLOW_EMPTY`
2. **ANTI-BLOCK:** mỗi `approver_role` phải `approverResolver.resolveFirst(role)` có user — không thì `NO_APPROVER`
3. `requestedBy` default `SystemUtils.getCurrentUsername()`
4. Lưu `ApprovalRequest` `status=PENDING`, `currentStepOrder` = bước 1
5. Pseudo-step `stepOrder=0`, `action=SUBMITTED`
6. Mỗi template step → `ApprovalStep` `PENDING` + username/person/name đã resolve

**Quan trọng:** `ApprovalCreator.create()` **không** publish `ApprovalDecidedEvent`. Chỉ `ApprovalServiceImpl.create` (HTTP) publish `ASSIGNED` ngay sau create.

---

## 8. `ApprovalServiceImpl` — duyệt / inbox

### 8.1 Approve / Reject

```
action(id, comment, approve=true|false)
  → request phải PENDING
  → isCurrentApprover(me): username khớp step hiện tại
        HOẶC ApproverResolver.userHasRole(me, step.approverRole)
  → cập nhật step APPROVED|REJECTED + comment + actionedAt
  → Reject: request REJECTED + event REJECTED
  → Approve:
        có bước PENDING tiếp theo → advance currentStepOrder + event ASSIGNED
        hết bước → request APPROVED + event APPROVED
```

### 8.2 Inbox `listMy`

| `status` param | Filter |
|----------------|--------|
| null / `"pending"` | Request `PENDING` ∧ user là current approver |
| khác | Mọi request related (requester hoặc xuất hiện trên bất kỳ step) |

### 8.3 Timeline / by-subject

- `timeline(id)`: steps theo `stepOrder`
- `timelineBySubject` / `findBySubject`: latest request theo `(subjectType, subjectId)` ordered `requestedAt DESC`

---

## 9. `ApproverResolver` — resolve người duyệt

**Không** đi org-chart (department manager). Resolve theo **role code QTHT**:

1. Tìm `Role` theo `code` (ignore case), not deleted
2. `UserRole` → `User` (bỏ `status == 0`)
3. Trả `ApproverHint(username, personId, displayName, roleCode)`
4. `resolveFirst` = user đầu tiên (MVP, không round-robin)

`userHasRole`: check roles user; **fallback demo:** username equals/contains roleCode (vd user `cfo` cho role `CFO`).

### Quan hệ với QLNS `LeaveApprovalResolver`

| Concern | `ApproverResolver` (approval) | `LeaveApprovalResolver` (qlns) |
|---------|-------------------------------|--------------------------------|
| Gán step lúc create | Role → user có role | Không gán step |
| Manager theo phòng ban | Không | `Person.department` → `Department.managerId` / deputy → User |
| HR | Role `HR` trên flow | Config `frezo.leave.hr-users` + permission |
| IDOR list leave | Không | Có |

**Hệ quả:** bước `MANAGER` trên flow seed = user có **role** `MANAGER`, không bắt buộc là QL trực tiếp của người xin nghỉ. QLNS vẫn resolve `managerUsername` riêng cho notify/IDOR.

---

## 10. Flow templates & seed

### 10.1 `ApprovalFlowServiceImpl`

- Create: generate `code`, nếu active → `deactivateOthers(subjectType, keepId)`
- Update: patch + optionally replace steps (soft-delete cũ)
- Một `subjectType` nên chỉ còn **một** `active=true`

### 10.2 `ApprovalFlowSeedRunner` (`@Order(50)`, idempotent theo `code`)

| Code | Name | subjectType | Steps |
|------|------|-------------|-------|
| `LEAVE_STANDARD` | Nghỉ phép chuẩn | `LEAVE` | 1 `MANAGER` → 2 `HR` |
| `PAYROLL_PERIOD` | Khoá kỳ lương | `PAYROLL` | 1 `CHIEF_ACC` → 2 `ADMIN` |
| `PURCHASE_REQUEST` | Yêu cầu mua hàng | `PURCHASE_REQUEST` | 1 `MANAGER` → 2 `ADMIN` |

Consumer constants (tham chiếu, runtime thường null flowCode):

- `LeaveApprovalBridge.FLOW_CODE = "LEAVE_STANDARD"`
- `PayrollPeriodServiceImpl.APPROVAL_FLOW_CODE = "PAYROLL_PERIOD"`
- Warehouse PR: `FLOW_CODE = "PURCHASE_REQUEST"` (cần verify tên field exact)

---

## 11. `ApprovalDecidedEvent` & listeners

### 11.1 Event fields

`requestId`, `subjectType`, `subjectId`, `status` (`ASSIGNED`|`APPROVED`|`REJECTED`), `actedBy`, `comment`.

Publish **chỉ** từ `ApprovalServiceImpl` (create HTTP / approve / reject).

### 11.2 `ApprovalNotificationListener` (trong module)

| Event | Hành vi |
|-------|---------|
| `ASSIGNED` | Notify assignee (username step hoặc `resolveAll(role)`); type `APPROVAL_ASSIGNED` |
| `APPROVED` / `REJECTED` | Notify `requestedBy`; type `APPROVAL_APPROVED` / `APPROVAL_REJECTED` |

Deep link gợi ý:

| subjectType | Link |
|-------------|------|
| `LEAVE` | `/qlns/leaves?highlight={subjectId}` |
| `PAYROLL` / `PAYROLL_PERIOD` | `/qlns/payroll-periods?id={subjectId}` |
| `PURCHASE_REQUEST` | `/warehouse/purchase-requests?id={subjectId}` |
| default | `/approvals?id={requestId}` |

### 11.3 Listeners ngoài module

| Listener | Module | Subject | Sync |
|----------|--------|---------|------|
| `QlnsApprovalSubjectListener` | qlns | `LEAVE`, `PAYROLL` (+ alias `PAYROLL_PERIOD`) | Leave / PayrollPeriod status |
| `PurchaseRequestApprovalListener` | warehouse | `PURCHASE_REQUEST` | PR APPROVED/REJECTED (+ stock alert) |

Chi tiết map leave/payroll: [module-qlns-bom.md](./module-qlns-bom.md) §18.

---

## 12. Module khác gắn Approval thế nào

### 12.1 Pattern chuẩn

```
1. Domain entity tạo/submit ở trạng thái chờ duyệt
2. approvalCreator.create(SubjectType.XXX.name(), entityId, summary, null, null, null)
3. entity.approvalRequestId = req.getId(); save
4. Listener @EventListener ApprovalDecidedEvent → cập nhật status domain
5. User duyệt tại POST /approvals/{id}/approve|reject (không gọi lại API domain cũ)
```

### 12.2 Callers `ApprovalCreator`

| Caller | Module | Khi nào |
|--------|--------|---------|
| `LeaveApprovalBridge.start` | qlns | Sau `LeaveRequest` create |
| `PayrollPeriodServiceImpl.lock` | qlns | Khoá kỳ lương |
| `PurchaseRequestServiceImpl.submit` | warehouse | Submit PR (nếu `warehouse.pr.approval.required=true`) |
| `ApprovalServiceImpl.create` | approval | `POST /approvals` |

### 12.3 Event gap khi create trực tiếp

Bridge QLNS/Warehouse gọi `ApprovalCreator` **không** fire `ASSIGNED` lúc tạo → `ApprovalNotificationListener` **không** chạy ngay lúc submit.

Bù đắp hiện có:

- Leave: `notifyManagerPending()` trong QLNS
- Payroll / PR: notify bước 1 phụ thuộc cơ chế khác hoặc chờ `ASSIGNED` khi approve chuyển bước — **cần verify** UX inbox bước đầu

HTTP `POST /approvals` thì có `ASSIGNED` ngay.

### 12.4 Không dùng engine (ví dụ QLNS)

Regularization, Resignation, Recognition redeem, Contract OP/RV nội bộ — duyệt trên API domain.

---

## 13. DB / migration

**Không** có Flyway `approval_*` dưới `module-server/.../db/migration/`.

Schema dựa JPA:

| Profile | `ddl-auto` (tham chiếu) |
|---------|-------------------------|
| Dev / docker | `update` |
| Prod | `validate` |

→ Prod phải có bảng sẵn khớp entity. Cột `approval_request_id` trên leave/payroll/PR nằm ở entity domain (ddl-auto hoặc migration riêng — **cần verify** từng bảng).

---

## 14. Cross-links

| Guild / module | Liên hệ |
|----------------|---------|
| [module-qlns-bom.md](./module-qlns-bom.md) | Leave + PayrollPeriod lock + listener sync |
| [module-qtht-bom.md](./module-qtht-bom.md) | Role / User gán `MANAGER`, `HR`, `CHIEF_ACC`, `ADMIN` |
| [module-common.md](./module-common.md) | `SubjectType`, `NotificationService`, permission annotation |
| [module-auth-bom.md](./module-auth-bom.md) | User status / UserRole |
| `module-warehouse-bom` | Purchase Request + listener (guild riêng nếu có sau) |
| [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) | Naming / audit / soft delete |

---

## 15. Checklist

### 15.1 Đọc code lần đầu

- [ ] 4 bảng: `approval_flow`, `approval_flow_step`, `approval_request`, `approval_step`
- [ ] `ApprovalFlowSeedRunner` — 3 flow chuẩn + role từng bước
- [ ] `ApprovalCreator.resolveFlow` + ANTI-BLOCK `NO_APPROVER`
- [ ] `ApproverResolver` = role-based (không phải org manager)
- [ ] `ApprovalServiceImpl.action` — ASSIGNED vs APPROVED vs REJECTED
- [ ] `GET /approvals/my` vs `POST /approvals/{id}/approve|reject`
- [ ] `QlnsApprovalSubjectListener` + `PurchaseRequestApprovalListener`
- [ ] Phân biệt create qua Creator (no event) vs HTTP create (có `ASSIGNED`)

### 15.2 Rule bắt buộc

| ✅ | ❌ |
|----|----|
| Một flow `active` / `subjectType` khi resolve runtime | Nhiều flow active cùng loại → resolve “first” không ổn định |
| Fail-fast `NO_APPROVER` lúc create | Tạo PENDING không ai thấy trong inbox |
| Domain sync qua `ApprovalDecidedEvent` | Set status leave/payroll “APPROVED” trực tiếp từ API cũ (410 leave) |
| Gán role QTHT cho user trước khi demo duyệt | Giả định bước `MANAGER` = QL phòng ban tự động |

### 15.3 Cần verify

- [ ] `CANCELLED` / `SKIPPED` có kế hoạch implement không
- [ ] Permission seed cho `/approvals` / `/approval-flows`
- [ ] Schema prod khi `ddl-auto=validate`
- [ ] Notify bước 1 sau Leave/Payroll/PR create (event gap)
- [ ] `ApprovalFlowDto` có trả `code` cho admin UI không

---

*Cập nhật khi thêm subjectType mới, đổi seed steps/roles, hoặc bắt buộc publish event khi gọi `ApprovalCreator` từ bridge.*
