# Frezo Backend — module-task-bom (Task / Ticket / Tag / Category)

> Module quản lý **công việc (tasks)**, **ticket hỗ trợ**, **nhãn (tags)** và **danh mục ticket**.
> Đọc cùng [README.md](./README.md) · [module-common.md](./module-common.md) · [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

Package gốc: `com.frezo.task`. Context path HTTP thường là `/api` (ví dụ `GET /api/task/task`).

---

## 1. Phạm vi module

| Domain | Bảng | Base path API |
|--------|------|---------------|
| Công việc | `tasks`, `task_tags` | `/task/task` |
| Ticket | `tickets` | `/task/ticket` |
| Nhãn | `tags` | `/task/tag` |
| Danh mục ticket | `ticket_categories` | `/task/ticket-category` |

**Không thuộc module này:** project/CRM deal, workflow phê duyệt (`module-approval-bom`), notification bell (gửi qua `NotificationService` ở common/server).

---

## 2. Class map

| Layer | Class | Vai trò |
|-------|-------|---------|
| Controller | `TaskController` | CRUD + assign / status / review |
| Controller | `TicketController` | CRUD + assign / status / review |
| Controller | `TagController` | CRUD nhãn |
| Controller | `TicketCategoryController` | CRUD danh mục + list active |
| Entity | `Task`, `Ticket`, `Tag`, `TicketCategory` | JPA → bảng tương ứng |
| Enum | `TaskStatusEnum`, `PriorityEnum` | Trạng thái / ưu tiên task |
| Enum | `Ticket.TicketStatus`, `Ticket.TicketPriority` | Trạng thái / ưu tiên ticket (inner enum) |
| Error | `TaskErrorCode` | `ErrorCode` riêng module |
| Security | (package `task.security`) | Rule truy cập task/ticket nếu có |

---

## 3. Entity & bảng

### 3.1 `Task` → `tasks`

Kế thừa `BaseEntity`.

| Field | Cột | Ý nghĩa |
|-------|-----|---------|
| `title` | `title` | Tiêu đề |
| `projectId` | `project_id` | Tham chiếu project (string id) |
| `assigneeId` | `assignee_id` | Người được giao |
| `description` | `description` | TEXT |
| `priority` | `priority` | `PriorityEnum`: HIGH / MEDIUM / LOW |
| `status` | `status` | `TaskStatusEnum` |
| `deadline` | `deadline` | Hạn |
| `tags` | join `task_tags` | Many-to-many → `tags` |
| `fileName` | `file_name` | File đính kèm (tên) |

**`TaskStatusEnum`:**

| Code | Ý nghĩa |
|------|---------|
| `OPEN` | Mở |
| `IN_PROGRESS` | Đang thực hiện |
| `DONE` | EU đánh dấu xong — chờ người giao / admin duyệt |
| `CLOSED` | Người giao đã xác nhận |
| `CANCELLED` | Hủy |

### 3.2 `Ticket` → `tickets`

| Field | Cột | Ý nghĩa |
|-------|-----|---------|
| `code` | `code` | Unique, VD `TICKET-0001` |
| `title` | `title` | Bắt buộc |
| `description` | `description` | TEXT |
| `status` | `status` | OPEN / IN_PROGRESS / RESOLVED / CLOSED |
| `priority` | `priority` | LOW / MEDIUM / HIGH / URGENT |
| `category` | `category` | Mã / tên danh mục (string) |
| `reporterId` | `reporter_id` | Người tạo |
| `assigneeId` | `assignee_id` | Người xử lý |
| `dueDate` | `due_date` | Hạn |
| `resolvedAt` | `resolved_at` | Thời điểm resolve |
| `resolutionNote` | `resolution_note` | Ghi chú giải quyết |

### 3.3 `Tag` → `tags`

| Field | Ý nghĩa |
|-------|---------|
| `code` | Mã nhãn |
| `name` | Tên hiển thị |
| `category` | Nhóm nhãn |
| `color` | Màu UI |

### 3.4 `TicketCategory` → `ticket_categories`

| Field | Cột | Ý nghĩa |
|-------|-----|---------|
| `code` | `code` | Unique, bắt buộc |
| `name` | `name` | Bắt buộc |
| `sortOrder` | `sort_order` | Thứ tự |
| `active` | `active` | Mặc định `true` |

---

## 4. API

Tất cả trả `ApiResponse<T>` (convention Frezo). Permission qua `@CheckPermission` / security module (xem [module-common.md](./module-common.md)).

### 4.1 Task — `/task/task`

| Method | Path | Hành vi |
|--------|------|---------|
| `POST` | `/task/task` | Tạo |
| `PUT` | `/task/task/{id}` | Cập nhật |
| `DELETE` | `/task/task/{id}` | Soft-delete |
| `GET` | `/task/task/{id}` | Chi tiết |
| `GET` | `/task/task` | Danh sách (filter/page) |
| `PATCH` | `/task/task/{id}/assign/{assigneeId}` | Giao việc |
| `PATCH` | `/task/task/{id}/status` | Đổi trạng thái |
| `POST` | `/task/task/{id}/review` | Duyệt hoàn thành (`DONE` → `CLOSED`) |

### 4.2 Ticket — `/task/ticket`

Cùng pattern: CRUD + assign + status + review.

### 4.3 Tag — `/task/tag`

| Method | Path |
|--------|------|
| `POST` / `PUT` / `DELETE` / `GET` | `/task/tag`, `/task/tag/{id}` |

### 4.4 Ticket category — `/task/ticket-category`

| Method | Path | Ghi chú |
|--------|------|---------|
| CRUD | `/task/ticket-category` | |
| `GET` | `/task/ticket-category/active` | Chỉ danh mục `active=true` |

---

## 5. Quy tắc nghiệp vụ (tóm tắt)

| Rule | Chi tiết |
|------|----------|
| ✅ Review | Chỉ người giao hoặc admin duyệt; entity phải ở `DONE` (task) / `RESOLVED` (ticket) |
| ✅ Complete | Chỉ assignee hoặc admin đánh dấu hoàn thành |
| ✅ Access | `TASK_ACCESS_DENIED` / `TICKET_ACCESS_DENIED` nếu không đủ quyền xem |
| ✅ Category | Ticket category code unique; validate trước khi gán |
| ❌ Đóng thẳng từ OPEN | Đi qua status hợp lệ; dùng `review` để chốt |

Error codes: `TaskErrorCode` implements `ErrorCode` → `GlobalExceptionHandler` ([module-common.md](./module-common.md)).

---

## 6. Liên kết module khác

| Module | Quan hệ |
|--------|---------|
| `module-common` | `BaseEntity`, `ApiResponse`, `CheckPermission`, `NotificationService` (nếu notify khi assign) |
| `module-qtht-bom` | User / Person / Menu permission |
| FE | `packages/erp` — task / ticket pages |

---

## 7. Checklist đọc code

- [ ] `TaskController` / `TicketController` — assign, status, review
- [ ] `TaskStatusEnum` vs `Ticket.TicketStatus` — khác nhau
- [ ] Join table `task_tags`
- [ ] `TicketCategory` active list
- [ ] `TaskErrorCode` — forbidden / invalid status

---

*Cập nhật khi đổi status machine, API path, hoặc rule review/assign.*
