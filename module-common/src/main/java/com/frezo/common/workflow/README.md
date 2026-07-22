# Workflow Engine

Engine chung để bất kỳ module business nào cũng có approval flow **customizable** —
không cần code hard-coded flow trong service.

## Kiến trúc 4 entity

```
┌─────────────────────┐        ┌──────────────────────┐
│ WorkflowDefinition  │◄───┐   │ WorkflowInstance     │
│ (template — admin   │    │   │ (đang chạy — 1 per   │
│  cấu hình qua UI)   │    │   │  entity business)    │
└─────────┬───────────┘    │   └─────────┬────────────┘
          │ 1..N           │             │ 1..N
          ▼                │             ▼
┌─────────────────────┐    │   ┌──────────────────────┐
│ WorkflowStep        │    │   │ WorkflowTask         │
│ (thứ tự + approver  │    │   │ (1 task cho mỗi step │
│  type + value)      │    │   │  đang xử lý)         │
└─────────────────────┘    │   └──────────────────────┘
                           │
    (snapshot code ─────── ┘  Instance chỉ lưu code, resolve
     tại runtime)             steps từ definition hiện tại.
```

## Approver types

| Type      | approverValue       | Task assignee                                      |
|-----------|---------------------|----------------------------------------------------|
| `USER`    | username            | 1 user cố định — chỉ user đó duyệt được            |
| `ROLE`    | role code           | Pool — bất kỳ user có role đó                      |
| `MANAGER` | (null)              | Auto = quản lý của requester (chưa impl — fallback ADMIN) |
| `ADMIN`   | (null)              | Pool — bất kỳ Admin                                |

## Cách tích hợp cho 1 module mới (VD `PurchaseOrder`)

### 1. Định nghĩa entityType constant

```java
public static final String WF_ENTITY_TYPE = "PURCHASE_ORDER";
public static final String WF_DEF_CODE   = "PURCHASE_ORDER_DEFAULT"; // seed hoặc admin tạo
```

### 2. Inject `WorkflowService` + gọi khi tạo entity

```java
@Autowired WorkflowService workflowService;

public PurchaseOrder create(PurchaseOrderRequest req) {
    PurchaseOrder po = poRepository.save(...);
    workflowService.start(
        WF_DEF_CODE,
        WF_ENTITY_TYPE,
        po.getId(),
        SystemUtils.getCurrentUsername(),
        "PO #" + po.getCode() + " - " + po.getSupplierName()
    );
    return po;
}
```

### 3. FE render progress qua endpoint

```
GET /wf/instances/by-entity/PURCHASE_ORDER/{poId}
```

Response gồm `tasks[]` + `steps[]` — plug vào `<WorkflowStepper />`.

### 4. Approve/reject buttons dùng chung endpoint

```
POST /wf/tasks/{taskId}/approve   { "comment": "OK" }
POST /wf/tasks/{taskId}/reject    { "reason": "..." }
```

FE lấy pending tasks trong inbox qua `GET /wf/tasks/mine`.

### 5. Sync trạng thái entity với workflow

Nếu entity cần đổi status (VD PO chuyển sang `APPROVED`), lắng nghe:
- Poll `getInstanceByEntity()` sau mỗi user action, hoặc
- (v2) Đăng ký `WorkflowListener` bean — engine sẽ callback khi instance COMPLETED / REJECTED.

## Ưu điểm

- **Không phải code lại approve/reject** cho mỗi module — dùng chung `/wf/tasks/*`
- **Admin customize được** flow qua UI `/qtht/workflows`:
  - Thêm/xoá bước
  - Reorder bằng arrow up/down
  - Đổi approver USER→ROLE khi tổ chức thay đổi
  - Bật SLA & allowSkip per step
- **Backward-compat**: module có thể dùng engine song song với hard-coded flow cũ,
  migrate dần khi ổn định.

## TODO (v2)

- [ ] `MANAGER` resolver — SPI `ApproverResolver` cho phép module qtht/qlns đăng ký
      logic tìm quản lý trực tiếp của requester
- [ ] `WorkflowListener` callback SPI khi instance COMPLETED/REJECTED
- [ ] Conditional branching (VD > 100tr đi CEO, < thì chỉ Finance) — cần expression evaluator
- [ ] Escalation khi quá SLA (tự động push next approver)
- [ ] Parallel approval (2 người cùng duyệt — collect vote)
