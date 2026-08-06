# Frezo Backend — module-crm-bom (CRM: Lead → Deal → Quote → Invoice + Hoa hồng + Sequence)

> Module CRM đầy đủ: Lead, Pipeline/Kanban Deal, Meeting, Activity, Quote, Invoice (hạch toán GL), Commission, Email Sequence (job stub).
> Đọc **cùng** [DATABASE_STANDARD.md](../DATABASE_STANDARD.md). Cross-module: customer (`customer_id`), accounting (`JournalService`), email (sequence gửi — hiện stub).

Package gốc: `com.frezo.crm`. Module Maven: `module-crm-bom`.

Context HTTP thường: `/api` → path dưới là `/api/crm/...`. Soft-delete qua `BaseEntity.isDeleted` (trừ khi ghi chú khác).

---

## 1. Vai trò & phạm vi

| Hạng mục | Chi tiết |
|----------|----------|
| Nghiệp vụ | Quản lý cơ hội bán: Lead → convert Deal → báo giá → hóa đơn → thu tiền → hoa hồng |
| Kanban | Deal gắn `pipeline_id` + `stage_id`; move stage / won / lost |
| Hoa hồng | Rule theo sale (`*` = mặc định 5%); accrue khi `recordPayment` |
| Email sequence | CRUD + enroll Lead; job `EMAIL_SEQUENCE` cron hourly — **stub send** (chỉ log + cập nhật step) |
| Không thuộc module | Master Customer/NCC (→ `module-customer-bom`); Sổ cái (→ `module-accounting-bom`) |

```
Lead.create / convert
  → Deal (pipeline default + stage)
       → Quote (items) → send / accept / reject
            → Invoice (items) → issue → postToGL → recordPayment
                 → CommissionService.accrueFromInvoice
Meeting / Activity gắn dealId | customerId
EmailSequence.enroll(lead) → EmailSequenceJob.processDueSteps (stub)
```

---

## 2. Class map

### 2.1 Controllers

| Controller | Base path | Vai trò |
|------------|-----------|---------|
| `LeadController` | `/crm/leads` | CRUD Lead + convert → Deal |
| `PipelineController` | `/crm/pipelines` | CRUD pipeline, stages, `ensure-default` |
| `DealController` | `/crm/deals` | CRUD Deal Kanban, move-stage, won, lost |
| `MeetingController` | `/crm/meetings` | CRUD cuộc họp |
| `ActivityController` | `/crm/activities` | Timeline theo deal/customer |
| `QuoteController` | `/crm/quotes` | CRUD báo giá + send/accept/reject |
| `InvoiceController` | `/crm/invoices` | CRUD HĐ + issue / post-to-gl / record-payment |
| `CommissionController` | `/crm/commissions` | Rules, entries, dashboard, approve/paid/void |
| `EmailSequenceController` | `/crm/email-sequences` | Sequence + enroll |

### 2.2 Services

| Interface | Impl | Vai trò |
|-----------|------|---------|
| `LeadService` | `LeadServiceImpl` | CRUD + `convert` tạo Deal |
| `PipelineService` | `PipelineServiceImpl` | CRUD + `ensureDefault` (6 stage seed) |
| `DealService` | `DealServiceImpl` | CRUD + moveStage / markWon / markLost |
| `MeetingService` | `MeetingServiceImpl` | CRUD meeting (status mặc định `SCHEDULED`) |
| `ActivityService` | `ActivityServiceImpl` | Log timeline |
| `QuoteService` | `QuoteServiceImpl` | Quote + items + trạng thái |
| `InvoiceService` | `InvoiceServiceImpl` | Invoice + GL + commission accrue |
| `CommissionService` | `CommissionServiceImpl` | Rule resolve, accrue, dashboard |
| `EmailSequenceService` | `EmailSequenceServiceImpl` | Sequence/steps/enroll + `processDueSteps` |

### 2.3 Entities → bảng

| Entity | Bảng | Ghi chú |
|--------|------|---------|
| `Lead` | `crm_lead` | status enum, score, owner, converted_* |
| `Pipeline` | `crm_pipeline` | `is_default`, `active` |
| `Stage` | `crm_stage` | `order_no`, `probability`, `won` (true/false/null) |
| `Deal` | `crm_deal` | amount VND, status OPEN/WON/LOST/STALLED |
| `DealActivity` | `crm_deal_activity` | CALL/EMAIL/MEETING/NOTE/TASK |
| `Meeting` | `crm_meeting` | SCHEDULED/DONE/CANCELLED |
| `Quote` | `crm_quote` | unique `code` |
| `QuoteItem` | `crm_quote_item` | line items |
| `Invoice` | `crm_invoice` | unique `code`, GL + commission snapshot |
| `InvoiceItem` | `crm_invoice_item` | line items |
| `CommissionRule` | `crm_commission_rule` | unique `salesperson_username`; `*` = default |
| `CommissionEntry` | `crm_commission_entry` | PENDING/APPROVED/PAID/VOID |
| `EmailSequence` | `crm_email_sequence` | name, active |
| `EmailSequenceStep` | `crm_email_sequence_step` | delay_days, subject, body_html |
| `EmailSequenceEnrollment` | `crm_email_sequence_enrollment` | ACTIVE/COMPLETED/… |

### 2.4 Enums & lỗi

| Class | Giá trị / key |
|-------|----------------|
| `LeadStatus` | `NEW`, `CONTACTED`, `QUALIFIED`, `UNQUALIFIED`, `CONVERTED` |
| `DealStatus` | `OPEN`, `WON`, `LOST`, `STALLED` |
| `QuoteStatus` | `DRAFT`, `SENT`, `ACCEPTED`, `REJECTED`, `EXPIRED` |
| `InvoiceStatus` | `DRAFT`, `ISSUED`, `PARTIALLY_PAID`, `PAID`, `VOID` |
| `ActivityType` | `CALL`, `EMAIL`, `MEETING`, `NOTE`, `TASK` |
| `CrmErrorCode` | `LEAD_NOT_FOUND`, `LEAD_ALREADY_CONVERTED`, `PIPELINE_NOT_FOUND`, `STAGE_NOT_FOUND`, `DEAL_NOT_FOUND`, `DEAL_ALREADY_CLOSED`, `QUOTE_NOT_FOUND`, `INVOICE_NOT_FOUND`, `INVOICE_ALREADY_POSTED`, `COMMISSION_NOT_FOUND`, `COMMISSION_INVALID` |

### 2.5 Job / seed

| Class | Vai trò |
|-------|---------|
| `EmailSequenceJob` | `SchedulableJob` code `EMAIL_SEQUENCE`, module `CRM`, cron `0 0 * * * *` |
| `CommissionRuleSeedRunner` | `@Order(55)` — seed rule `*` = 5% nếu chưa có |

---

## 3. API map (permission `@CheckPermission`)

### 3.1 Leads — `/crm/leads`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `GET` | `/crm/leads` | VIEW | Query `status` \| `owner` (ưu tiên status) |
| `GET` | `/crm/leads/{id}` | VIEW | |
| `POST` | `/crm/leads` | CREATE | Body `LeadRequest` |
| `PUT` | `/crm/leads/{id}` | UPDATE | |
| `DELETE` | `/crm/leads/{id}` | DELETE | Soft-delete |
| `POST` | `/crm/leads/{id}/convert` | CREATE | Params: `pipelineId`, `stageId`, `customerId`, `amount` → `{ dealId }` |

### 3.2 Pipelines — `/crm/pipelines`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `GET` | `/crm/pipelines` | VIEW | |
| `GET` | `/crm/pipelines/{id}` | VIEW | |
| `GET` | `/crm/pipelines/{id}/stages` | VIEW | Order by `order_no` |
| `POST` | `/crm/pipelines` | CREATE | Inline stages trong request |
| `PUT` | `/crm/pipelines/{id}` | UPDATE | |
| `DELETE` | `/crm/pipelines/{id}` | DELETE | Soft-delete |
| `POST` | `/crm/pipelines/ensure-default` | CREATE | Tạo pipeline mặc định + 6 stage nếu chưa có |

### 3.3 Deals — `/crm/deals`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `GET` | `/crm/deals` | VIEW | **Bắt buộc** một trong: `pipelineId` / `status` / `owner` / `customerId` — không filter → `[]` |
| `GET` | `/crm/deals/{id}` | VIEW | |
| `POST` | `/crm/deals` | CREATE | Auto pipeline/stage default nếu thiếu |
| `PUT` | `/crm/deals/{id}` | UPDATE | |
| `DELETE` | `/crm/deals/{id}` | DELETE | Soft-delete |
| `PATCH` | `/crm/deals/{id}/move-stage` | UPDATE | `toStageId` — sync probability + WON/LOST theo `stage.won` |
| `PATCH` | `/crm/deals/{id}/won` | UPDATE | status WON, probability 100 |
| `PATCH` | `/crm/deals/{id}/lost` | UPDATE | `reason?` |

### 3.4 Meetings — `/crm/meetings`

| Method | Path | Action |
|--------|------|--------|
| `GET` | `/crm/meetings` | VIEW (`dealId?`) |
| `GET` | `/crm/meetings/{id}` | VIEW |
| `POST` | `/crm/meetings` | CREATE |
| `PUT` | `/crm/meetings/{id}` | UPDATE |
| `DELETE` | `/crm/meetings/{id}` | DELETE (softDelete) |

### 3.5 Activities — `/crm/activities`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `GET` | `/crm/activities` | VIEW | `dealId` \| `customerId` — không filter → `[]` |
| `POST` | `/crm/activities` | CREATE | Body `ActivityRequest` |
| `DELETE` | `/crm/activities/{id}` | DELETE | |

### 3.6 Quotes — `/crm/quotes`

| Method | Path | Action |
|--------|------|--------|
| `GET` | `/crm/quotes` | VIEW |
| `GET` | `/crm/quotes/{id}` | VIEW → `{ quote, items }` |
| `POST` | `/crm/quotes` | CREATE |
| `PUT` | `/crm/quotes/{id}` | UPDATE |
| `DELETE` | `/crm/quotes/{id}` | DELETE |
| `PATCH` | `/crm/quotes/{id}/send` | UPDATE → SENT |
| `PATCH` | `/crm/quotes/{id}/accept` | UPDATE → ACCEPTED |
| `PATCH` | `/crm/quotes/{id}/reject` | UPDATE → REJECTED |

### 3.7 Invoices — `/crm/invoices`

| Method | Path | Action | Ghi chú |
|--------|------|--------|---------|
| `GET` | `/crm/invoices` | VIEW | |
| `GET` | `/crm/invoices/{id}` | VIEW | `{ invoice, items }` |
| `POST` | `/crm/invoices` | CREATE | Apply commission fields |
| `PUT` | `/crm/invoices/{id}` | UPDATE | Chặn nếu đã `glJournalEntryId` |
| `DELETE` | `/crm/invoices/{id}` | DELETE | Soft-delete |
| `PATCH` | `/crm/invoices/{id}/issue` | UPDATE | DRAFT → ISSUED |
| `POST` | `/crm/invoices/{id}/post-to-gl` | CREATE | Nợ 131 / Có doanh thu → lưu `gl_journal_entry_id` |
| `POST` | `/crm/invoices/{id}/record-payment` | CREATE | Params `amount`, `paymentAccountCode?` (default `1121`) + accrue HH |

### 3.8 Commissions — `/crm/commissions`

| Method | Path | Action |
|--------|------|--------|
| `GET` | `/crm/commissions/dashboard` | VIEW |
| `GET` | `/crm/commissions/rules` | VIEW |
| `POST` | `/crm/commissions/rules` | CREATE (upsert theo username) |
| `DELETE` | `/crm/commissions/rules/{id}` | DELETE — **không** xoá username `*` |
| `GET` | `/crm/commissions/resolve-rate` | VIEW | `salespersonUsername?` |
| `GET` | `/crm/commissions/entries` | VIEW | filter sale |
| `PATCH` | `/crm/commissions/entries/{id}/approve` | UPDATE |
| `PATCH` | `/crm/commissions/entries/{id}/mark-paid` | UPDATE |
| `PATCH` | `/crm/commissions/entries/{id}/void` | UPDATE |

### 3.9 Email sequences — `/crm/email-sequences`

| Method | Path | Action |
|--------|------|--------|
| `GET` | `/crm/email-sequences` | VIEW |
| `POST` | `/crm/email-sequences` | CREATE (+ steps) |
| `PUT` | `/crm/email-sequences/{id}` | UPDATE (replace steps nếu gửi) |
| `POST` | `/crm/email-sequences/{id}/enroll` | CREATE | Body `{ leadId }` |

---

## 4. Entity fields chính

### 4.1 Lead (`crm_lead`)

| Field | Ý nghĩa |
|-------|---------|
| `fullName`, `phone`, `email`, `companyName` | Liên hệ |
| `source` | FB / Google / Referral / Website / Import… |
| `status` | `LeadStatus` |
| `score` | 0–100 |
| `ownerUsername` | Sale phụ trách |
| `convertedCustomerId` / `convertedDealId` | Sau convert |

### 4.2 Deal (`crm_deal`)

| Field | Ý nghĩa |
|-------|---------|
| `title`, `pipelineId`, `stageId` | Kanban |
| `customerId` | Có thể null trước khi gắn KH |
| `amount`, `currency` | Mặc định VND |
| `probability` | Override từ stage |
| `expectedCloseDate` / `closedDate` | |
| `status`, `ownerUsername`, `lostReason` | |

### 4.3 Invoice + Commission snapshot

| Field Invoice | Ý nghĩa |
|---------------|---------|
| `code`, `customerId`, `quoteId` | Liên kết |
| `subtotal` / `taxAmount` / `discountAmount` / `total` / `paidAmount` | Tiền |
| `status` | DRAFT…PAID |
| `glJournalEntryId` | Đã post GL |
| `salespersonUsername` | Sale nhận HH |
| `commissionRatePercent` / `commissionAmount` | Snapshot ước tính |

| Field CommissionEntry | Ý nghĩa |
|-----------------------|---------|
| `baseAmount` | Thường = `paidAmount` (cập nhật khi thu thêm) |
| `ratePercent`, `commissionAmount`, `itemQuantity` | |
| `status` | PENDING → APPROVED → PAID \| VOID |

### 4.4 Email sequence

| Enrollment status | Ý nghĩa |
|-------------------|----------|
| `ACTIVE` | Đang chạy |
| `COMPLETED` | Hết bước |
| `PAUSED` / `CANCELLED` | (field hỗ trợ; job chỉ xử lý ACTIVE) |

---

## 5. Luồng chính

### 5.1 Convert Lead → Deal

```
POST /crm/leads/{id}/convert
  → LeadServiceImpl.convert
       ├─ Guard: status != CONVERTED
       ├─ pipeline = pipelineId ? get : ensureDefault()
       ├─ stage = first match stageId (hoặc first stage)
       ├─ Tạo Deal OPEN, title = fullName [– company], currency VND
       └─ Lead → CONVERTED + convertedDealId / convertedCustomerId
```

| Rule | |
|------|--|
| ✅ Convert lần 2 → `LEAD_ALREADY_CONVERTED` | |
| ✅ Không tự tạo Customer — FE/API truyền `customerId` nếu có | |
| ❌ List Deal không filter → luôn `[]` (không dump toàn DB) | |

### 5.2 Pipeline mặc định (`ensureDefault`)

| order_no | Tên | probability | won |
|----------|-----|-------------|-----|
| 0 | Tiềm năng | 10 | null |
| 1 | Đủ điều kiện | 30 | null |
| 2 | Đề xuất / Báo giá | 60 | null |
| 3 | Đàm phán | 80 | null |
| 4 | Chốt Won | 100 | true |
| 5 | Mất Lost | 0 | false |

`moveStage`: nếu `stage.won == true` → WON; `false` → LOST; else OPEN + cập nhật probability.

### 5.3 Quote → Invoice → GL → Payment → Hoa hồng

```
Quote DRAFT → send → SENT → accept → ACCEPTED
Invoice create (applyCommissionFields)
  → issue (ISSUED)
  → postToGL (Nợ 131 / Có DT; set glJournalEntryId)
  → recordPayment(amount)
       ├─ paidAmount += amount → PARTIALLY_PAID | PAID
       ├─ Journal: Nợ 1121 (hoặc paymentAccountCode) | Có 131
       └─ commissionService.accrueFromInvoice (catch warn nếu lỗi)
```

**Resolve rate:** personal active rule → rule `*` → fallback `5.00`.

**Resolve salesperson:** request override → Deal.ownerUsername qua Quote.dealId.

| Rule | |
|------|--|
| ✅ Accrue theo `paidAmount` (partial OK); không ghi đè entry PAID/VOID | |
| ✅ Update invoice đã post GL → `INVOICE_ALREADY_POSTED` | |
| ❌ Không xoá commission rule username `*` | |

### 5.4 Email sequence (stub)

```
create/update sequence + steps
enroll(leadId) → status ACTIVE, currentStepOrder=0
EmailSequenceJob.execute → processDueSteps
  → với mỗi ACTIVE: next step = current+1
       → log.info stub send (chưa gọi EmailService)
       → lastSentAt = now; COMPLETED nếu hết bước
```

| Rule | |
|------|--|
| ⚠️ `delayDays` lưu trên step nhưng **job hiện không check delay** — gửi lần lượt mỗi lần chạy | |
| ❌ Chưa wire `module-email-bom` | |

---

## 6. Dependencies (Maven)

| Artifact | Lý do |
|----------|-------|
| `module-common` | `BaseEntity`, `ApiResponse`, `CheckPermission`, `AppException`, `SchedulableJob` |
| `module-customer-bom` | Tham chiếu `customer_id` (không inject service bắt buộc trong convert) |
| `module-accounting-bom` | `JournalService` post GL / thu tiền |
| `spring-boot-starter-web` | Controllers |

---

## 7. Cross-links

| Module / file | Liên hệ |
|---------------|---------|
| [module-customer-bom.md](./module-customer-bom.md) | `customerId` trên Deal/Invoice/Meeting |
| [module-product-bom.md](./module-product-bom.md) | `product_id` trên QuoteItem/InvoiceItem (snapshot name) |
| [module-email-bom.md](./module-email-bom.md) | Mục tiêu gửi thật cho sequence (chưa nối) |
| `module-accounting-bom` | `PostingSource`, `JournalService.createAndPost` |
| [DATABASE_STANDARD.md](../DATABASE_STANDARD.md) | Naming `crm_*`, soft-delete, UUID |

```
[FE ERP CRM]
  → /api/crm/leads|deals|pipelines|quotes|invoices|commissions|meetings|activities|email-sequences
[Accounting]
  ← InvoiceServiceImpl.postToGL / recordPayment
[Scheduler]
  → EmailSequenceJob (EMAIL_SEQUENCE)
```

---

## 8. Checklist

### 8.1 Đọc code lần đầu

- [ ] Controllers dưới `com.frezo.crm.controller` — 9 class
- [ ] `LeadServiceImpl.convert` + `PipelineServiceImpl.ensureDefault`
- [ ] `DealServiceImpl.moveStage` / won / lost
- [ ] `InvoiceServiceImpl.recordPayment` + `CommissionServiceImpl.accrueFromInvoice`
- [ ] `CommissionRuleSeedRunner` (`*` = 5%)
- [ ] `EmailSequenceServiceImpl.processDueSteps` + `EmailSequenceJob`
- [ ] Enums trong `com.frezo.crm.common` + `CrmErrorCode`

### 8.2 Rule bắt buộc

| ✅ | ❌ |
|----|----|
| Soft-delete Lead/Deal/Quote/Invoice | Hard-delete business data |
| Filter Deal/Activity khi list | Gọi GET deals không param rồi expect data |
| Accrue HH khi thu tiền (có salesperson) | Expect HH khi invoice chưa paid / thiếu sale |
| Không xoá rule `*` | Xoá default rồi hệ thống mất % |
| Ghi nhớ sequence = stub | Expect mail thật từ CRM job |

### 8.3 Kiểm thử thủ công gợi ý

- [ ] `ensure-default` → có 6 stage
- [ ] Convert lead → deal OPEN trên stage đầu
- [ ] move-stage sang “Chốt Won” → status WON
- [ ] Tạo invoice + record-payment → entry PENDING trên `/crm/commissions/entries`
- [ ] Enroll sequence + chạy job → `currentStepOrder` tăng / COMPLETED

---

*Cập nhật khi wire email sequence thật, đổi schema hoa hồng, hoặc đổi tài khoản GL mặc định (131 / 1121).*
