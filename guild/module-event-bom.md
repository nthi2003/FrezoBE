# Frezo Backend — module-event-bom (Sự kiện & RSVP)

> Module **sự kiện nội bộ**: admin CRUD / publish / cancel; portal nhân viên xem lịch + RSVP.
> Đọc cùng [README.md](./README.md) · [module-common.md](./module-common.md) · [DATABASE_STANDARD.md](../DATABASE_STANDARD.md).

Package gốc: `com.frezo.event`. Context path HTTP thường là `/api`.

---

## 1. Phạm vi module

| Góc nhìn | Base path | Ai dùng |
|----------|-----------|---------|
| Admin | `/events` | Tạo, sửa, publish, cancel, xem đăng ký |
| Portal | `/events/portal` | User xem sự kiện published, RSVP, danh sách của tôi |

**Không thuộc module này:** livestream marketing (`/mkt/live` trong `module-fbautomation-bom`), calendar HR leave.

---

## 2. Class map

| Layer | Class | Vai trò |
|-------|-------|---------|
| Controller | `EventController` | Admin API |
| Controller | `EventPortalController` | Portal + RSVP |
| Service | `EventService` / `EventServiceImpl` | Toàn bộ nghiệp vụ |
| Entity | `Event` | Bảng `evt_event` |
| Entity | `EventRegistration` | Bảng `evt_registration` |
| DTO | `EventSaveRequest`, `RsvpRequest`, `EventDto`, `EventRegistrationDto` | Request / response |

---

## 3. Entity & bảng

### 3.1 `Event` → `evt_event`

Kế thừa `BaseEntity`. Index: `idx_evt_status`, `idx_evt_start`.

| Field | Cột | Ý nghĩa |
|-------|-----|---------|
| `title` | `title` | Bắt buộc |
| `description` | `description` | Tối đa ~4000 |
| `location` | `location` | Địa điểm |
| `startAt` | `start_at` | Bắt buộc |
| `endAt` | `end_at` | Có thể null |
| `status` | `status` | `DRAFT` / `PUBLISHED` / `CANCELLED` |
| `capacity` | `capacity` | `null` = không giới hạn |
| `registeredCount` | `registered_count` | Đếm denorm |
| `coverUrl` | `cover_url` | Ảnh cover |
| `organizerUsername` | `organizer_username` | Người tổ chức |
| `publishedAt` | `published_at` | Khi publish |
| `cancelledAt` | `cancelled_at` | Khi cancel |

Constants trong `EventServiceImpl`:

| Constant | Value |
|----------|-------|
| `STATUS_DRAFT` | `DRAFT` |
| `STATUS_PUBLISHED` | `PUBLISHED` |
| `STATUS_CANCELLED` | `CANCELLED` |

### 3.2 `EventRegistration` → `evt_registration`

Unique: `(event_id, username)` — `uk_evt_reg_event_user`.

| Field | Cột | Ý nghĩa |
|-------|-----|---------|
| `eventId` | `event_id` | FK logic → event |
| `username` | `username` | User RSVP |
| `displayName` | `display_name` | Tên hiển thị |
| `email` | `email` | Email |
| `rsvpStatus` | `rsvp_status` | GOING / MAYBE / DECLINED / CANCELLED |
| `note` | `note` | Ghi chú |
| `registeredAt` | `registered_at` | Thời điểm đăng ký |

| Constant RSVP | Value |
|---------------|-------|
| `RSVP_GOING` | `GOING` |
| `RSVP_MAYBE` | `MAYBE` |
| `RSVP_DECLINED` | `DECLINED` |
| `RSVP_CANCELLED` | `CANCELLED` |

---

## 4. API Admin — `/events`

| Method | Path | Hành vi |
|--------|------|---------|
| `GET` | `/events` | List admin (filter `status`) |
| `GET` | `/events/calendar` | Lịch theo khoảng `from` / `to` |
| `GET` | `/events/{id}` | Chi tiết |
| `POST` | `/events` | Tạo (thường `DRAFT`) |
| `PUT` | `/events/{id}` | Cập nhật |
| `DELETE` | `/events/{id}` | Soft-delete |
| `POST` | `/events/{id}/publish` | `DRAFT` → `PUBLISHED`, set `publishedAt` |
| `POST` | `/events/{id}/cancel` | → `CANCELLED`, set `cancelledAt` |
| `GET` | `/events/{id}/registrations` | Danh sách RSVP |

---

## 5. API Portal — `/events/portal`

| Method | Path | Hành vi |
|--------|------|---------|
| `GET` | `/events/portal` | Sự kiện đã publish (portal) |
| `GET` | `/events/portal/my` | RSVP của user hiện tại |
| `GET` | `/events/portal/{id}` | Chi tiết (chỉ published) |
| `POST` | `/events/portal/{id}/rsvp` | Đăng ký / đổi RSVP |
| `DELETE` | `/events/portal/{id}/rsvp` | Hủy RSVP |

### 5.1 RSVP — rule

| Rule | Chi tiết |
|------|----------|
| ✅ Chỉ event `PUBLISHED` | Portal không RSVP draft/cancelled |
| ✅ Capacity | Nếu `capacity` khác null và đủ chỗ — từ chối GOING mới |
| ✅ Unique user/event | Upsert theo `(event_id, username)` |
| ✅ `registeredCount` | Cập nhật khi GOING thêm/bớt |
| ❌ RSVP event cancelled | Từ chối |

---

## 6. Luồng trạng thái event

```
DRAFT ──publish──► PUBLISHED ──cancel──► CANCELLED
  │                    │
  └──────cancel────────┘ (tuỳ impl — thường cancel từ published)
```

| Hành động | Điều kiện thường gặp |
|-----------|----------------------|
| Publish | Đang `DRAFT`, có `startAt` hợp lệ |
| Cancel | Đang `PUBLISHED` (hoặc draft) → `CANCELLED` |
| Delete | Soft-delete; không xóa cứng registration (audit) |

---

## 7. Checklist đọc code

- [ ] `EventController` vs `EventPortalController` — tách quyền admin / user
- [ ] `EventServiceImpl` — constants status + RSVP
- [ ] Unique `uk_evt_reg_event_user`
- [ ] Capacity + `registeredCount` khi RSVP GOING
- [ ] Calendar query theo `start_at`

---

*Cập nhật khi đổi status/RSVP enum, capacity rule, hoặc path portal.*
