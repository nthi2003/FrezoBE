# Frezo Workflow Engine — Design Document

> Status: **DRAFT v0.1** · Target: Q3 2026
> Author: AI Agent + Frezo team
> Scope: generic approval / process orchestration cho toàn bộ ERP

---

## 1. Bối cảnh & Motivation

### 1.1 Vấn đề hiện tại

Frezo có nhiều module cần workflow duyệt nhưng mỗi module lại tự implement:

| Module | Workflow hiện tại | Vấn đề |
|---|---|---|
| **Nghỉ phép** (`LeaveRequest`) | 2 cấp cứng: QL → HR (hardcode trong service) | Không sửa được cấu hình, phải deploy code mới |
| **Hợp đồng** (`Contract`) | 1 cấp: manager duyệt (chỉ owner logic đơn giản) | Không hỗ trợ multi-step |
| **Bảng lương** (`Payroll`) | 2 trạng thái: draft → confirmed → paid | Không có approval chain |
| **Ticket** (`Ticket`) | Assign chain đơn giản | Không có SLA / escalation |
| **Bài viết** (`Article`) | Author → Manager review | Cứng, khó thêm bước |
| **Tương lai**: PO, YCTM, tuyển dụng... | Chưa có | Sẽ tiếp tục fragment nếu không có nền chung |

**Duplicate code**: mỗi module tự viết `status enum`, `notify approver`, `history log`, `permission check`, `deep-link URL`. → khó maintain, khó thêm feature (VD: escalation nếu quá 24h chưa duyệt).

### 1.2 Mục tiêu

- **1 engine cho tất cả**: mọi entity cần duyệt đều dùng chung API.
- **Cấu hình runtime**: admin sửa flow qua UI, không cần deploy.
- **Multi-step + Conditional routing**: hỗ trợ if-else, parallel approval, dynamic assignee (theo dept head, theo cost center...).
- **SLA + Escalation**: quá X giờ chưa duyệt → tự chuyển cho ai đó / gửi email urgent.
- **Audit trail đầy đủ**: ai làm gì khi nào, không có gap.
- **Không phá module cũ**: coexist với `LeaveRequestServiceImpl` hiện tại, migrate dần.

---

## 2. So sánh giải pháp

### 2.1 Sử dụng framework có sẵn

| Framework | Ưu điểm | Nhược điểm cho Frezo |
|---|---|---|
| **Camunda 8 / 7** | BPMN 2.0 chuẩn, mạnh nhất, có visual designer | Kích thước lớn (200MB+), setup Zeebe/Postgres/Elastic phức tạp, license C8 (SaaS/commercial), độ dốc học tập cao |
| **Flowable** | Fork Camunda 7, nhẹ hơn, open source | Vẫn nặng, BPMN XML khó config qua UI đơn giản |
| **Activiti** | Old-school, stable | Ít cập nhật, cộng đồng nhỏ dần |
| **jBPM (RedHat)** | Full BPM suite | Quá phức tạp cho use case Frezo |
| **Netflix Conductor** | Cloud-native, event-driven | Overkill, orient cho microservices workflow (order/pay) |
| **Temporal** | Durable workflows, code-first | Yêu cầu Temporal cluster riêng, học tập cao |
| **n8n / Node-RED** | Low-code visual | Không native Java, khó embed |

### 2.2 Custom lightweight (đề xuất)

**Lý do chọn custom**:
- **Scope hẹp**: Frezo chỉ cần "approval chain có điều kiện + timer + notify", KHÔNG cần BPMN full spec (gateway complex, subprocess, message correlation...).
- **Tích hợp sâu**: đã có sẵn `Person`, `Department`, `User`, `NotificationService`, `Role` — engine chỉ cần orchestrate.
- **UX cấu hình**: designer BPMN quá phức tạp cho admin business. Cần UI đơn giản kiểu Airtable Automation / Notion (drag drop step).
- **Không phá stack**: giữ nguyên Spring Boot + Postgres, không thêm broker (Zeebe/Kafka) nếu chưa cần.
- **Migration path**: nếu sau này scale lớn (> 1M workflow/day) → có thể swap engine (extract interface `WorkflowExecutor`).

---

## 3. Kiến trúc high-level

```
┌────────────────────────────────────────────────────────────────────┐
│                        FE (React)                                  │
│  ┌────────────────┐   ┌────────────────┐   ┌────────────────┐    │
│  │ Admin: Workflow │   │ User: My Tasks │   │ Any Module:    │    │
│  │  Designer       │   │  (bell inbox)   │   │  Timeline widget│    │
│  └────────┬───────┘   └────────┬───────┘   └────────┬───────┘    │
└───────────┼─────────────────────┼─────────────────────┼────────────┘
            │                     │                     │
            ▼                     ▼                     ▼
┌────────────────────────────────────────────────────────────────────┐
│                    BE — module-workflow                            │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │  WorkflowDefinitionController  │  WorkflowTaskController      │ │
│  │  WorkflowInstanceController    │  WorkflowHistoryController   │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                          │                                         │
│  ┌───────────────────────▼──────────────────────────────────────┐ │
│  │  WorkflowEngine — core orchestrator                          │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │ │
│  │  │ StepExecutor   │  │ AssigneeResolver│  │ ConditionEval  │ │ │
│  │  │  (approve/     │  │  (dept head,   │  │  (SpEL / JEXL) │ │ │
│  │  │   auto/timer)  │  │   role, expr)   │  │                │ │ │
│  │  └────────────────┘  └────────────────┘  └────────────────┘ │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐ │ │
│  │  │ NotifierAdapter│  │ SlaTracker     │  │ HistoryLogger  │ │ │
│  │  │  (bell/email)  │  │  (@Scheduled)  │  │  (audit trail) │ │ │
│  │  └────────────────┘  └────────────────┘  └────────────────┘ │ │
│  └──────────────────────────────────────────────────────────────┘ │
│                          │                                         │
└──────────────────────────┼─────────────────────────────────────────┘
                           ▼
             ┌────────────────────────────┐
             │  Postgres — workflow_*     │
             │  workflow_definition       │
             │  workflow_step             │
             │  workflow_instance         │
             │  workflow_task             │
             │  workflow_history          │
             └────────────────────────────┘
                           │
                           ▼
             ┌──────────────────────────────┐
             │  Business entities           │
             │  (LeaveRequest, Contract,    │
             │   Payroll, Ticket, ...)      │
             │  ← link qua entityType+id    │
             └──────────────────────────────┘
```

**Nguyên tắc thiết kế**:
- Engine **KHÔNG** owns business data. Chỉ giữ workflow state + reference `entityType + entityId`.
- Business module chỉ cần: (1) publish event khi entity submitted, (2) subscribe callback khi workflow completed.
- Communicate qua **domain event** (Spring `ApplicationEventPublisher`) — loose coupling.

---

## 4. Data model

### 4.1 `workflow_definition` (config)

```sql
id                 uuid PK
code               varchar(50) UNIQUE   -- 'LEAVE_APPROVAL_V2'
name               varchar(255)
description        text
entity_type        varchar(50)          -- 'LEAVE' | 'CONTRACT' | ...
active             boolean
version            int                   -- v1, v2, v3 (không sửa in-place)
created_by         varchar(100)
created_at         timestamp
```

Nhiều version → khi update flow, tạo v2 mới; instance chạy dở vẫn dùng v1.

### 4.2 `workflow_step` (bước trong flow)

```sql
id                 uuid PK
definition_id      uuid FK
step_order         int
name               varchar(255)          -- 'QL trực tiếp duyệt'
type               varchar(30)           -- APPROVAL | NOTIFY | AUTO | CONDITION | TIMER
assignee_type      varchar(30)           -- USER | ROLE | MANAGER_OF_REQUESTER | DEPT_HEAD | EXPR
assignee_value     varchar(500)          -- 'admin' | 'ROLE_HR' | SpEL expr
sla_hours          int                   -- SLA (null = no SLA)
sla_action         varchar(30)           -- ESCALATE | AUTO_APPROVE | NOTIFY_URGENT
on_approve_step_id uuid                  -- next step khi approve
on_reject_step_id  uuid                  -- 'END' hoặc step khác
condition_expr     text                  -- SpEL — skip step nếu false
config             jsonb                 -- extra: {parallel: true, quorum: 2, ...}
```

Ví dụ config cho LEAVE:
```
step 1: type=APPROVAL, assignee=MANAGER_OF_REQUESTER, sla=48h
        on_approve → step 2, on_reject → END
step 2: type=APPROVAL, assignee=ROLE_HR, sla=24h, condition=(days > 3)
        on_approve → END, on_reject → END
```

### 4.3 `workflow_instance` (runtime)

```sql
id                 uuid PK
definition_id      uuid FK
definition_version int
entity_type        varchar(50)           -- 'LEAVE'
entity_id          varchar(36)           -- FK sang leave_request.id
current_step_id    uuid
status             varchar(20)           -- RUNNING | COMPLETED | REJECTED | CANCELLED
started_by         varchar(100)
started_at         timestamp
completed_at       timestamp
metadata           jsonb                 -- snapshot data cần cho condition (VD: leave.days)
```

### 4.4 `workflow_task` (đơn task cần user act)

```sql
id                 uuid PK
instance_id        uuid FK
step_id            uuid FK
assigned_to        varchar(100)          -- username
status             varchar(20)           -- PENDING | APPROVED | REJECTED | EXPIRED | SKIPPED
due_at             timestamp             -- SLA deadline
acted_at           timestamp
comment            text
```

Query "my tasks": `SELECT * FROM workflow_task WHERE assigned_to = ? AND status = 'PENDING'`.

### 4.5 `workflow_history` (audit)

```sql
id                 uuid PK
instance_id        uuid FK
step_id            uuid
action             varchar(30)           -- START | APPROVE | REJECT | SKIP | ESCALATE | COMPLETE
actor_username     varchar(100)
actor_role         varchar(30)
comment            text
created_at         timestamp
```

Tương đương `LeaveRequestHistory` hiện tại — nhưng generic.

---

## 5. Assignee Resolvers (extensible)

Interface `AssigneeResolver`:

```java
interface AssigneeResolver {
    String type(); // 'USER' | 'ROLE' | 'MANAGER_OF_REQUESTER' | ...
    List<String> resolve(WorkflowInstance instance, WorkflowStep step);
}
```

Built-in impls:
| Type | Logic |
|---|---|
| `USER` | Trả `step.assigneeValue` (username tĩnh) |
| `ROLE` | Query users có role code = value |
| `MANAGER_OF_REQUESTER` | `Person(instance.startedBy) → Department.managerId → User.username` |
| `DEPT_HEAD` | Head của department chỉ định |
| `EXPR` | Evaluate SpEL: `#{person.jobLevel > 5 ? 'ceo' : 'director'}` |
| `CUSTOM` | Plugin do module cung cấp |

Đăng ký resolver qua Spring:
```java
@Component
class ManagerOfRequesterResolver implements AssigneeResolver { ... }
```

Engine auto-inject `Map<String, AssigneeResolver>`.

---

## 6. State machine — flow chạy như thế nào

```
1. Business event: LeaveRequestService.create()
   → publish WorkflowStartEvent(entityType=LEAVE, entityId=xxx, definitionCode='LEAVE_APPROVAL_V2')

2. WorkflowEngine handles event:
   a. Load definition + steps
   b. Create WorkflowInstance (status=RUNNING, currentStep=step1)
   c. Execute step1:
      - Evaluate condition_expr → nếu false, skip sang next step
      - Resolve assignees → tạo WorkflowTask(s)
      - Send notification (via NotifierAdapter)
      - Ghi history: START
   d. Return

3. User approves task:
   POST /workflow/tasks/{id}/approve
   → engine: task.status=APPROVED
   → history: APPROVE
   → engine: move to step.onApproveStepId
   → recursion: execute step2

4. Terminal state:
   - Nếu step.onApproveStepId == 'END' → instance.status=COMPLETED
   - Publish WorkflowCompletedEvent → business module subscribe

5. Business callback (LeaveRequestService):
   @EventListener
   void onWorkflowCompleted(WorkflowCompletedEvent e) {
       if (e.getEntityType().equals("LEAVE")) {
           leaveRepo.updateStatus(e.getEntityId(), "APPROVED");
       }
   }
```

---

## 7. SLA & Escalation (Scheduler)

`@Scheduled(fixedRate = 60_000)` — chạy mỗi phút:
```
1. Query WorkflowTask WHERE status=PENDING AND due_at < NOW()
2. Cho từng task, execute sla_action của step:
   - ESCALATE: reassign lên manager của assignee, notify urgent
   - AUTO_APPROVE: task.status=APPROVED, tiến tiếp
   - NOTIFY_URGENT: gửi email + push notification
```

---

## 8. API contract (draft)

### 8.1 Admin — Workflow Definition

```
POST   /workflow/definitions                 tạo mới
PUT    /workflow/definitions/{id}            update
POST   /workflow/definitions/{id}/publish    activate + tăng version
GET    /workflow/definitions?entityType=LEAVE  list
```

### 8.2 Runtime — Instance & Task

```
POST   /workflow/instances/start             { definitionCode, entityType, entityId, metadata }
GET    /workflow/instances/{id}              status + current step
POST   /workflow/instances/{id}/cancel

GET    /workflow/tasks/my                    tasks assigned to current user
POST   /workflow/tasks/{id}/approve          { comment }
POST   /workflow/tasks/{id}/reject           { reason }
POST   /workflow/tasks/{id}/reassign         { newAssignee, reason }

GET    /workflow/instances/{id}/history      timeline
```

---

## 9. FE UI — Admin Designer

**Không dùng BPMN visual** (quá kỹ thuật). Thay vào đó: **step list dạng Airtable Automation**:

```
Workflow: "Duyệt đơn nghỉ phép"
─────────────────────────────────────────────────────────────
① Người xin gửi đơn                                    [icon]
                                                         │
                                                         ▼
② APPROVAL · QL trực tiếp                          ⚙️ Edit
   Assignee: Manager của người xin
   SLA: 48h → Escalate lên trưởng bộ phận
   ✅ Duyệt → next   ❌ Từ chối → END
                                                         │
                                                         ▼
③ APPROVAL · HR chốt                                ⚙️ Edit
   Assignee: Role HR
   Condition: Chỉ chạy nếu số ngày > 3
   SLA: 24h → Notify urgent
   ✅ Duyệt → END    ❌ Từ chối → END

[+ Thêm bước]
```

Mỗi step click → drawer edit: chọn type, assignee, SLA, condition.

**Preview mode**: nhập test data → engine chạy dry-run → show đường dẫn sẽ đi.

---

## 10. Migration path — không phá module cũ

### Phase 1 (Q3 2026)
- Build `module-workflow` với schema đầy đủ
- Admin UI cơ bản (list/create/edit definition)
- Runtime API + SLA scheduler
- Notify integration

### Phase 2 (Q3-Q4 2026)
- **Migrate LeaveRequest**: giữ `LeaveRequestServiceImpl` cũ nhưng thêm option `useNewEngine=true` (feature flag). Test song song.
- Seed definition mặc định `LEAVE_APPROVAL_V2` (giống flow hiện tại) — user không thấy khác biệt.

### Phase 3 (Q4 2026)
- Migrate Contract, Article, Ticket lần lượt
- Xoá hardcode approval logic ở các service

### Phase 4 (2027)
- Extend: parallel approval (nhiều người cùng duyệt, quorum), subprocess (workflow con), form-based dynamic input

---

## 11. Non-goals (v1)

Cố ý KHÔNG làm ở v1 để giữ scope nhỏ:
- ❌ BPMN 2.0 XML import/export
- ❌ Visual designer (SVG canvas kéo thả)
- ❌ Message correlation (chờ external event)
- ❌ Subprocess / call activity
- ❌ Multi-tenant workflow (mỗi org 1 flow) — v1 chỉ 1 tenant
- ❌ Chạy custom Java code trong workflow (chỉ SpEL cho condition)

---

## 12. Risks & Mitigation

| Risk | Mitigation |
|---|---|
| Engine trở nên "one big service" | Split rõ ràng: `Executor`, `Resolver`, `Notifier`, `Scheduler` — mỗi class < 300 dòng |
| Migration break existing leave data | Feature flag `useNewEngine`. Rollback dễ. Test bằng shadow mode (chạy song song, so kết quả) |
| Admin config sai flow → all requests stuck | Preview mode + validation (phát hiện dead-end step) + kill switch: admin có thể "force complete" |
| Performance khi có 10K+ pending tasks | Index `assigned_to`, partition table `workflow_history` theo tháng |
| SLA scheduler drift | Dùng distributed lock (ShedLock) nếu deploy multi-instance |

---

## 13. Timeline & Effort

| Phase | Duration | Effort |
|---|---|---|
| 1. Core engine + schema + API | 4 tuần | 1 dev BE |
| 2. Admin UI (definition designer) | 2 tuần | 1 dev FE |
| 3. Migrate LeaveRequest | 1 tuần | 1 dev fullstack |
| 4. Migrate Contract + Article + Ticket | 3 tuần | 1 dev fullstack |
| 5. SLA escalation + email urgent | 1 tuần | 1 dev BE |
| **Total** | **~11 tuần** | **2-3 dev** |

---

## 14. Next steps

1. **Review doc** với team → confirm scope v1
2. **PoC** (1 tuần):
   - Tạo `module-workflow` skeleton
   - Schema + 1 definition seeded (`LEAVE_APPROVAL_V2`)
   - API `/workflow/instances/start` chạy được 1 flow đơn giản
   - Test: giả lập tạo LeaveRequest → engine chạy đúng thứ tự step → gửi notification
3. **Ra quyết định** build hay dùng Flowable/Camunda dựa trên PoC
4. Nếu build custom → chia phase theo §10

---

## Appendix A: So sánh với LeaveRequestServiceImpl hiện tại

| Aspect | Hiện tại (module-qlns) | Workflow Engine (đề xuất) |
|---|---|---|
| Cấu hình | Hardcode trong Java | Sửa qua Admin UI |
| Số bước | 2 (fixed) | 1..N |
| Điều kiện skip | Không | Có (SpEL) |
| SLA | Không | Có |
| Escalation | Không | Có |
| Multi-recipient | Không (1 manager) | Có (parallel, quorum) |
| Audit trail | `LeaveRequestHistory` (custom) | `WorkflowHistory` (generic) |
| Reusable | ❌ mỗi module tự viết | ✅ 1 engine cho tất cả |
| Complexity | Đơn giản | Trung bình |

---

## Appendix B: Từ vựng

- **Definition**: bản thiết kế workflow (config)
- **Instance**: 1 lần thực thi của definition với entity cụ thể
- **Step**: 1 bước trong definition
- **Task**: 1 việc cần user act (thuộc về 1 step trong 1 instance)
- **Assignee**: user được giao task
- **SLA**: Service Level Agreement — deadline
- **Escalation**: chuyển task lên cấp cao hơn khi quá SLA
